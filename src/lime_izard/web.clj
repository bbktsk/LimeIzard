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
            [liberator.core :refer [resource defresource]]))

(defn db-spec
  []
  (env :database-url (str "postgres:///" (env :user))))

(def empty-user (apply hash-map :active false (interleave [:first_name
                                                           :last_name
                                                           :mood
                                                           :message
                                                           :photo_url
                                                           :fb_id]
                                                          (repeat ""))))

(defqueries "lime_izard/queries.sql" {:connection (db-spec)})

(defn user-get
  [id]

  (first (q-user-get {:fb_id id})))

(defn user-update
  [id data]
  (if-let [current (user-get id)]
    (q-user-update! (assoc (merge current data) :fb_id id))
    (q-user-insert! (assoc (merge empty-user data) :fb_id id))))


(defresource r-user-get [id]
  :available-media-types ["application/json"]
  :exists? (fn [ctx] (if-let [x (user-get id)] {:user x}))
  :handle-ok :user)

(defresource r-user-update [id data]
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :handle-ok (user-update id data))

(defroutes app
  (context "/api" []
           (POST "/users/:id"
                 {{id :id} :params body :body}
                 (r-user-update id (keywordize-keys body)))
           (GET "/users/:id"
                [id]
                (r-user-get id)))

  ;;(route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))


(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (-> #'app
                         wrap-json-response
                         wrap-json-body
                         (wrap-defaults (assoc site-defaults :security {:anti-forgery false}))
                         )
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
