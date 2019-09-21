(defproject riemann-console "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library]]

                 [amalloy/ring-buffer "1.3.0"]
                 [compojure "1.6.1"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [duratom "0.4.3"]
                 [haslett "0.1.6"]
                 [metosin/muuntaja "0.6.4"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.9"]
                 [ring "1.7.1"]
                 [thheller/shadow-cljs "2.8.52"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :aliases {"dev"  ["with-profile" "dev" "run" "-m" "shadow.cljs.devtools.cli" "watch" "app"]
            "prod" ["with-profile" "prod" "run" "-m" "shadow.cljs.devtools.cli" "release" "app"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]]
    :main riemann-console.server}

   :prod {}

   :uberjar {:omit-source  true
             :main         riemann-console.server
             :aot          [riemann-console.server]
             :uberjar-name "riemann-console.jar"
             :prep-tasks   ["compile" ["prod"]]}})
