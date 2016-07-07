(ns tern-validate.plugin
  (:require [leiningen.core.main]
            [leiningen.core.project])
  (:require [clojure.java.io :as io])
  (:require [tern-validate.core :as tv])
  (:import [java.io File])
  )

(defn- read-raw-from-stream
  "Read project file without loading certificates, plugins, middleware, etc."
  [stream]
  (let [tempfile (File/createTempFile "tern-validate-" ".clj")]
    (try (do (io/copy stream tempfile)
             (leiningen.core.project/read-raw (.getAbsolutePath tempfile)))
         (finally (when (.exists tempfile) (.delete tempfile))))))

(defn get-plugin-version
  []
  (let [res (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) "META-INF/leiningen/cc.artifice/tern-validate/project.clj")
        project (read-raw-from-stream res)]
    [(symbol (if (:group project) (:group project) (:name project))
             (:name project))
     (:version project)]))


(defn middleware
  [project]
  (let [version (tv/get-database-schema-version-in-repl)
        dependency (get-plugin-version)]
    (let [new-project (if version
                        (do ;;(leiningen.core.main/info (format "Setting schema version to %s" version))
                            (-> project
                                (assoc-in [:manifest "Database-Schema-Version"] version) ))
                        (do ;;(leiningen.core.main/warn (format "Unable to determine database schema version."))
                            project))]
      (if (some #(= dependency %) (get-in new-project [:dependencies]))
        new-project
        (leiningen.core.project/merge-profiles new-project [{:dependencies [dependency]}])))))

