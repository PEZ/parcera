(defproject carocad/parcera "0.1.1"
  :description "Grammar-based Clojure(script) parser"
  :url "https://github.com/carocad/parcera"
  :license {:name "LGPLv3"
            :url  "https://github.com/carocad/parcera/blob/master/LICENSE.md"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [instaparse/instaparse "1.4.10"]]
  :profiles {:dev {:dependencies [[criterium/criterium "0.4.5"] ;; benchmark
                                  [org.clojure/test.check "0.10.0"]]
                   :plugins      [[jonase/eastwood "0.3.5"]
                                  [lein-cljsbuild "1.1.7"]]
                   :hooks [leiningen.cljsbuild]
                   :cljsbuild {:builds
                               [{:id "dev"
                                 :source-paths ["src" "test"]
                                 :compiler {:main parcera.test-runner
                                            :output-to "target/out/tests.js"
                                            :target :nodejs
                                            :optimizations :none}}]
                               :test-commands
                               {"dev" ["node" "target/out/tests.js"]}}}
             :provided {:dependencies [[org.clojure/clojurescript "1.10.520"]]}}
  :test-selectors {:default     (fn [m] (not (some #{:benchmark} (keys m))))
                   :benchmark   :benchmark}
  :cljsbuild {:builds
              [{:source-paths ["src"]
                :compiler {:output-dir "target/out"
                           :optimizations :advanced}}]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
