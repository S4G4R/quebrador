(ns quebrador.main
  (:require [clojure.string :as string]
            [babashka.fs :as fs]
            [babashka.cli :as cli]
            [hexdump.core :as hex])
  (:gen-class))


(defn ^:private little-endian-str->big-endian-str
  [hex-string]
  (->> (partition-all 2 hex-string)
       reverse
       (map (partial apply str))
       string/join))


(defn zip-file?
  "Returns `true` if the given file is a ZIP file, `false` otherwise."
  [file]
  (-> (hex/hexdump-lines file {:offset 0 :size 4})
      first
      (string/split #" ")
      ;; Discard offset
      rest
      ;; Take first 4 bytes (groups of 2)
      (#(take 2 %))
      string/join
      little-endian-str->big-endian-str
      (#(str "0x" %))
      read-string
      (= 0x04034b50)))


(defn show-help
  "Print help menu"
  [spec]
  (println
   (cli/format-opts (merge spec {:order (vec (keys (:spec spec)))}))))


(def cli-spec
  "Defines the Babashka spec for the command line arguments"
  {:spec
   {:file {:coerce :string
           :desc "zip file to crack"
           :alias :f
           :validate #(and (fs/exists? %) (zip-file? %))
           :require true}}
   :error-fn
   (fn [{:keys [type cause msg option]}]
     (when (= :org.babashka/cli type)
       (case cause
         :require
         (println
          (format "Missing required argument: --%s\n" (name option)))
         :validate
         (println
          (format "%s does not exist or is not a zip file!\n" msg))))
     (System/exit 1))})


(defn -main [& args]
  (if (or (:help (cli/parse-opts args)) (:h (cli/parse-opts args)))
    (show-help cli-spec)
    (let [_opts (cli/parse-opts args cli-spec)]
      (println "Has zip headers."))))
