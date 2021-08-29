(ns codesmith.blocks.pedestal
  (:require [codesmith.blocks :as cb]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defmethod ig/init-key ::server [_ {:keys [service-map]}]
  (-> service-map
      http/create-server
      http/start))

(defmethod ig/halt-key! ::server [_ server]
  (http/stop server))

(defn assoc-server [ig-config]
  (assoc ig-config ::server {:service-map (ig/ref ::service-map)}))

(defmethod ig/init-key ::default-service-map
  [_ {:keys [base-service-map routes-fn]}]
  (-> {::http/host  "0.0.0.0"
       ;; do not block thread that starts web server
       ::http/join? false}
      (merge base-service-map)
      (merge {::env         ::default
              ::http/routes (routes-fn)})
      http/default-interceptors))

(defmethod cb/typed-block-transform
  [::pedestal ::default]
  [block-key system+profile ig-config final-substitution]
  [(-> ig-config
       assoc-server
       (assoc ::default-service-map (-> system+profile block-key)))
   final-substitution])

(derive ::default-service-map ::service-map)

(defmethod ig/init-key ::dev-service-map
  [_ {:keys [base-service-map routes-fn]}]
  (-> base-service-map
      (merge {::env                  ::dev
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes          #(route/expand-routes (routes-fn))
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ::http/secure-headers  {:content-security-policy-settings {:object-src "'none'"}}})
      http/default-interceptors
      http/dev-interceptors))

(defmethod cb/typed-block-transform
  [::pedestal ::dev]
  [block-key system+profile ig-config final-substitution]
  [(-> ig-config
       assoc-server
       (assoc ::dev-service-map (-> system+profile block-key)))
   final-substitution])

(derive ::dev-service-map ::service-map)
