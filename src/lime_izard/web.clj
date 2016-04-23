(ns lime-izard.web
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as db]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY context]]
            [compojure.route :as route]
            [crypto.random :as random]
            [environ.core :refer [env]]
            [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cookies :only [wrap-cookies]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [slingshot.slingshot :refer [throw+]]
            [yesql.core :refer [defqueries]]))

(defn db-spec
  []
  (env :database-url (str "postgres:///" (env :user))))

(def user-fields [:first_name :last_name :mood :message :photo_url :fb_id :sex
                  :active])

(def visit-fields [:beacon_uuid :signal :longitude :latitude])

(defn empty-map [fields]
  (apply hash-map (interleave fields (repeat nil))))

(defn ensure-fields [m fields]
  (merge (empty-map fields) m))

(defqueries "lime_izard/queries.sql" {:connection (db-spec)})

(defn user-get
  [id]

  (first (q-user-get {:fb_id id})))

(defn user-update
  [id data]
  (if-let [current (user-get id)]
    (q-user-update! (assoc (merge current data) :fb_id id))
    (throw+ "internal error, user vanished")))

(defn user-create!
  [data]
  (q-user-insert! (ensure-fields data user-fields)))

(defn new-user-valid?
  [data]
  (println "checking" data)
  (:fb_id data))

(defn visit-valid?
  [visit]
  (and (every? visit visit-fields)
       (= (count visit-fields) (count (keys visit)))))

(defn beacon-by-uuid
  [uuid]
  (first (q-beacon-by-uuid {:uuid uuid })))

(defn keep-latest
  [s]
  (->> s
       (group-by :fb_id)
       vals
       (map (fn [s] (sort-by :timestamp s)))
       (map last)
       (map (fn [x] (dissoc x :timestamp)))))

(defn handle-visit
  [id visit]
  (let [beacon-uuid (:beacon_uuid visit)
        beacon (beacon-by-uuid beacon-uuid)
        beacon-id (:id beacon)
        _ (println beacon-uuid beacon-id beacon)]
    (q-insert-visit! (assoc visit :fb_id id :beacon_id beacon-id))
    {:people (keep-latest (q-get-nearby {:beacon_id beacon-id :self id}))}))

(defn handle-poke
  [id target]
  (q-insert-poke! {:source id :target target}))

(defn handle-get-user-pokes
  [id]
  (let [pokes (q-get-user-pokes {:self id})]
    (q-mark-pokes-seen! {:self id})
    pokes))

(defresource r-user-get [id]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (if-let [x (user-get id)] {:user x}))
  :handle-ok :user)

(defresource r-user-get-beacons [id]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (if-let [x (user-get id)] {:user x}))
  :handle-ok (fn [_] (q-user-beacons {:owner id})))

(defresource r-user-create [data]
  ;;; FIXME should check for existing user
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :malformed? (fn [_] (not (new-user-valid? data)))
  :post! (fn [_] (user-create! data)))

(defresource r-user-update [id data]
  :allowed-methods [:put]
  :available-media-types ["application/json"]
  :exists? (fn [_] (user-get id))
  :put! (fn [_] (user-update id data)))

(defresource r-user-visit [id visit]
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (user-get id))
  :malformed? (fn [ctx] (not (visit-valid? visit)))
  :post! (fn [_] {:result (handle-visit id visit)})
  :handle-created :result)

(defresource r-user-poke [id target]
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (user-get id))
  :malformed? (fn [ctx] (println target)(not (user-get target)))
  :post! (fn [_] (handle-poke id target)))

(defresource r-user-get-pokes [id]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (user-get id))
  :handle-ok (handle-get-user-pokes id))

(defroutes app
  (context "/api" []
           (POST "/users"
                 {body :body}
                 (r-user-create (keywordize-keys body)))
           (PUT "/users/:id"
                 {{id :id} :params body :body}
                 (r-user-update id (keywordize-keys body)))
           (GET "/users/:id"
                [id]
                (r-user-get id))
           (GET "/users/:id/beacons"
                [id]
                (r-user-get-beacons id))
           (POST "/users/:id/visit"
                 {{id :id} :params body :body}
                 (r-user-visit id (keywordize-keys body)))
           (POST "/users/:id/poke"
                 {{id :id} :params body :body}
                 (r-user-poke id (body "target"))))

  ;;(route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))


(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (-> #'app
                         (wrap-trace :header :ui)
                         wrap-json-response
                         wrap-json-body
                         (wrap-defaults (assoc site-defaults :security {:anti-forgery false}))
                         )
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
