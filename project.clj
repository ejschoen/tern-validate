(defproject i2kconnect/tern-validate "0.1.0"
  :description "Insert tern database schema version into the project manifest."
  :url "http://bitbucket.org/i2kconnect/tern-validate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories ^:replace [["releases" {:url "https://prod.i2kconnect.com/artifactory/libs-release"
                                        :username "i2kdev" :password "AP4WKeyKBALnrFLRQcTvgip6zndxT1k2pmqWAu"}]
                           ["snapshots" {:url "https://prod.i2kconnect.com/artifactory/libs-snapshot"
                                         :username "i2kdev" :password "AP4WKeyKBALnrFLRQcTvgip6zndxT1k2pmqWAu"}]]
  :dependencies [[korma "0.4.4" :exclusions [org.clojure/clojure]]]
  :eval-in-leiningen true)
