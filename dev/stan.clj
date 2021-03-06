(ns stan
  (:require [integrant.repl :as ir]
            [integrant.repl.state :as irs]
            [codesmith.blocks.pedestal :as cbp]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [ring.util.response :as ring-resp]
            [codesmith.blocks :as cb]))

(defn home-page [_]
  (ring-resp/response "Hello World"))

(def routes
  #{["/" :get home-page :route-name :hello-world]})

(def base-service-map
  {;; Uncomment next line to enable CORS support, add
   ;; string(s) specifying scheme, host and port for
   ;; allowed source(s):
   ;;
   ;; "http://localhost:8080"
   ;;
   ;;::http/allowed-origins ["scheme://host:port"]

   ;; Tune the Secure Headers
   ;; and specifically the Content Security Policy appropriate to your service/application
   ;; For more information, see: https://content-security-policy.com/
   ;;   See also: https://github.com/pedestal/pedestal/issues/499
   ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
   ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
   ;;                                                          :frame-ancestors "'none'"}}

   ;; Root for resource interceptor that is available by default.
   ;;::http/resource-path     "/public"

   ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
   ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
   ::http/type              :jetty
   ;;::http/host "localhost"
   ::http/port              8080
   ;; Options to pass to the container (Jetty)
   ::http/container-options {:h2c? true
                             :h2?  false
                             ;:keystore "test/hp/keystore.jks"
                             ;:key-password "password"
                             ;:ssl-port 8443
                             :ssl? false
                             ;; Alternatively, You can specify you're own Jetty HTTPConfiguration
                             ;; via the `:io.pedestal.http.jetty/http-configuration` container option.
                             ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                             }})

(def system
  {:application :block-pedestal
   :blocks      [::cbp/pedestal]})

(def profile
  {:environment   :stan-dev
   ::cbp/pedestal {:type             ::cbp/dev
                   :base-service-map base-service-map
                   :routes-fn        #(deref #'routes)}})

(ir/set-prep! (constantly (cb/system->ig system profile)))

(comment

  (response-for
    (-> irs/system ::cbp/server :io.pedestal.http/service-fn)
    :get
    "/")

  (ir/go)
  (ir/halt)

  )
