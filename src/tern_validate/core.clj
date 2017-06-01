(ns tern-validate.core
  ;;(:use [korma.core :exclude [update]])
  (:require [clojure.java.io :as io])
  (:import [java.io File])
  (:import [java.util Properties])
  (:import [java.util.jar JarFile Manifest]))

(defn- read-raw
  "Simplistic project reader"
  [file]
  (let [p (read-string (slurp file))]
    (if (map? p)
      p
      (let [project-name (second p)
            artifact (name project-name)
            group (or (namespace project-name) artifact)
            keys (take-nth 2 (drop 3 p))
            vals (take-nth 2 (drop 4 p))]
        (into {} (map (fn [k v] [k v]) keys vals))))))
    

(defn read-raw-from-stream
  "Read project file without loading certificates, plugins, middleware, etc."
  [stream]
  (let [tempfile (File/createTempFile "tern-validate-" ".clj")]
    (try (do (io/copy stream tempfile)
             (read-raw (.getAbsolutePath tempfile)))
         (finally (when (.exists tempfile) (.delete tempfile))))))


(defn get-database-schema-version-in-repl
  []
  (let [files (try (sort (map #(.getName %)
                              (filter #(.endsWith (.getName %) ".edn") (file-seq (io/file "migrations/")))))
                   (catch Exception e (println (format "Failed to get migration files: %s" (.getMessage e))) nil))
        last-file (last files)
        version (when last-file (second (re-matches #"(\d+)-(.*)" last-file)))]
    version))

(defn get-database-schema-version-from-project
  [project-name]
  (let [res-enum (.getResources (.getContextClassLoader (Thread/currentThread))
                                (if (re-matches #".*-project.clj" project-name)
                                  project-name (format "%s-project.clj" project-name)))]
    (loop []
      (when (.hasMoreElements res-enum)
        (let [url (.nextElement res-enum)]
          (let [is (.openStream url)]
            (if is
              (let [project (read-string (slurp is))
                    version (get-in project [:manifest "Database-Schema-Version"])]
                (.close is)
                (if version
                  version
                  (recur)))
              (recur))))))))

(defn get-database-schema-version-from-manifest
  []
  (let [res-enum (.getResources (.getContextClassLoader (Thread/currentThread)) (JarFile/MANIFEST_NAME))]
    (loop []
      (when (.hasMoreElements res-enum)
        (let [url (.nextElement res-enum)]
          (let [is (.openStream url)]
            (if is
              (let [manifest (Manifest. is)
                    main-attributes (.getMainAttributes manifest)
                    version (.getValue main-attributes "Database-Schema-Version")]
                (.close is)
                ;;(println "FOUND MANIFEST")
                (if version
                  version
                  (do #_(println "NO VERSION") (recur))))
              (do #_(println "COULDN'T OPEN MANIFEST STREAM") (recur)))))))))

(defn get-database-schema-version-at-compile
  [project-name]
  (or (get-database-schema-version-from-manifest)
      (and project-name (get-database-schema-version-from-project project-name))
      (get-database-schema-version-in-repl)))
            

(defn get-db-project
  []
  (let [res-enum (.getResources (.getContextClassLoader (Thread/currentThread)) "project.clj")]
    (loop []
      (when (.hasMoreElements res-enum)
        (let [url (.nextElement res-enum)]
            (let [is (.openStream url)]
              (if is
                (let [project (read-raw-from-stream is)]
                  (.close is)
                  (if (:tern project)
                    project
                    (recur)))
                (recur))))))))
                    
(defn valid-version?
  "Validate a version number.  Version is a string.  Second argument is a map with :min-version and :max-version, which can be nil.
  Calls the optional callback function with a map as its argument. Map entries are:
   :version -- the version being validated
   :min-version -- minimum allowed version
   :max-version -- maximum allowed version
   :validation -- a keyword: :ok :too-old, :too-new, :mismatch.
  Returns true or false."
  [version {:keys [min-version max-version]} callback]
  {:pre [version]}
  (let [new-enough? (or (nil? min-version)
                        (<= (compare min-version version) 0))
        old-enough? (or (nil? max-version)
                        (>= (compare max-version version) 0))]
    (when callback
      (callback {:version version :min-version min-version :max-version max-version
                 :validation (cond (not new-enough?)
                                   :too-old
                                   (not old-enough?)
                                   :too-new
                                   :else :ok)}))
    (and new-enough? old-enough?)))

     
(defn explain-validation
  "Can be called from a callback function for validate, to generate an English string explaining the validation status."
  [{:keys [version] :as m}]
  (case (:validation m)
    :ok (cond (and (:min-version m) (:max-version m))
              (format "The database version %s fits within the expected range %s to %s" version (:min-version m) (:max-version m))
              (:min-version  m)
              (format "The database version %s is greater or equal to the expected version %s" version (:min-version m))
              (:max-version m)
              (format "The database version %s is less or equal to the expected version %s" version (:max-version m))
              :else (format "The database version %s is equal to the expected version %s" version version))
    :too-old (format "The database version %s is older than the minimum expected version %s" version (:min-version m))
    :too-new (format "The database version %s is newer than the maximum expected version %s" version (:max-version m))
    :mismatch (format "The database version %s does not match the expected version %s" version (:min-version m))
    :error (format (format "An exception was thrown: %s" (.getMessage (:exception m))))))

(defn validate2
  "Validate the database version. runtime-version is the current version of the database 
   (i.e., the max of the version column of the schema_versions table).
   Calls the optional callback function with a map as its argument. Map entries are:
   :version -- the version being validated
   :min-version -- minimum allowed version
   :max-version -- maximum allowed version
   :validation -- a keyword: :ok :too-old, :too-new, :mismatch.
  Returns true or false."
  [runtime-version project-name & [callback]]
  {:pre [runtime-version]}
  (let [compile-version (get-database-schema-version-at-compile project-name)]
    (if-let [validation (get-in (get-db-project) [:tern :validation])]
      (try (valid-version? runtime-version validation callback)
           (catch Exception e
             (when callback
               (callback {:version runtime-version
                          :min-version (:min-version validation)
                          :max-version (:max-version validation)
                          :validation :error
                          :exception e}))
             nil))
      (let [validation (= compile-version runtime-version)]
        (when callback
          (callback {:version runtime-version :min-version compile-version :max-version compile-version
                     :validation (if validation :ok :mismatch)}))
        validation))))

(defn validate
  [runtime-version & [callback]]
  (validate2 runtime-version nil callback))
