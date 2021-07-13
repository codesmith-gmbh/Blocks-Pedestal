(ns codesmith.blocks.pedestal
  (:require [codesmith.blocks :as cb]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defmethod ig/init-key ::default-service-map
  [_ {:keys [base-service-map routes-var]}]
  (-> base-service-map
      (merge {::env         :default
              ::http/routes @routes-var})
      http/default-interceptors))

(defmethod cb/typed-block-transform
  [::service-map :default]
  [block-key system+profile ig-config]
  (assoc ig-config ::default-service-map (-> system+profile block-key)))

(derive ::default-service-map ::service-map)

(defmethod ig/init-key ::dev-service-map
  [_ {:keys [base-service-map routes-var]}]
  (-> base-service-map
      (merge {::env                  :dev
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes          #(route/expand-routes @routes-var)
              ;; do not block thread that starts web server
              ::http/join?           false
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ::http/secure-headers  {:content-security-policy-settings {:object-src "'none'"}}})
      http/default-interceptors
      http/dev-interceptors))

(defmethod cb/typed-block-transform
  [::service-map :dev]
  [block-key system+profile ig-config]
  (assoc ig-config ::dev-service-map (-> system+profile block-key)))

(derive ::dev-service-map ::service-map)

(defmethod ig/init-key ::server [_ {:keys [service-map]}]
  (-> service-map
      http/create-server
      http/start))

(defmethod ig/halt-key! ::server [_ server]
  (http/stop server))

(defmethod cb/block-transform ::server
  [_ system+profile ig-config]
  (assoc ig-config ::server (merge
                              {:service-map (ig/ref ::service-map)}
                              (-> system+profile ::server))))