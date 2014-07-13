(defproject flickr-search "0.1.0-SNAPSHOT"
  :description "Search public flickr photos"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.5"]]
  :main flickr-search.core
  :aot [flickr-search.core])
