(ns lime-izard.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY context]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cookies :only [wrap-cookies]]
            [environ.core :refer [env]]
            [crypto.random :as random]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as db]
            [slingshot.slingshot :refer [throw+]]
            [clojure.walk :refer [keywordize-keys]]
            [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]))

(defn db-spec
  []
  (env :database-url (str "postgres:///" (env :user))))

(def user-fields [:first_name :last_name :mood :message :photo_url :fb_id :sex
                  :active])

(def visit-fields [:beacon_id :signal :longitude :latitude])

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
  (println data)
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


(defn handle-visit
  [id visit]
  (println visit)
  )

(defresource r-user-get [id]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (if-let [x (user-get id)] {:user x}))
  :handle-ok :user)

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
  :post! (fn [_] (handle-visit id visit)))


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
           (POST "/users/:id/visit"
                 {{id :id} :params body :body}
                 (r-user-visit id (keywordize-keys body))))

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
