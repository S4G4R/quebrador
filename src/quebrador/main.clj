(ns quebrador.main
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.java.io :as io]
            [clojure.string :as string]
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
           :require true}
    :dict {:coerce :string
           :desc "dictionary file containing list of words"
           :alias :d
           :validate #(and (fs/exists? %) (= (fs/extension %) "txt"))}
    :verbose {:coerce :boolean
              :alias :v}}
   :error-fn
   (fn [{:keys [type cause msg option]}]
     (when (= :org.babashka/cli type)
       (case cause
         :require
         (println
          (format "Missing required argument: --%s\n" (name option)))
         :validate
         (condp = option
           :file
           (println
            (format "%s does not exist or is not a zip file!\n" msg))
           :dict
           (println
            (format "%s does not exist or is not a text file!\n" msg)))))
     (System/exit 1))})


(defn password-correct?
  "Given a (password protected) ZIP file and a password, returns `true`
  if the password correctly decrypts the file, `false` otherwise."
  [file password]
  (try
    (-> (process/shell {:out :nil
                        ;; Suppress errors
                        :err :nil
                        :continue true}
                       "unzip"
                       "-p"
                       "-P" password
                       file)
        :exit
        (= 0))
    ;; Supress exception on shutdown
    (catch IllegalStateException _)))


(defn matching-password
  "Returns a password that successfully decrypts the given zip file
  using a dictionary attack, `nil` otherwise."
  [{dictionary :dict verbose? :verbose} zip]
  (with-open [reader (io/reader dictionary)]
    (loop [passwords (line-seq reader)]
      (when-let [password (first passwords)]
        (when verbose?
          (println "Trying:" password))
        (if (password-correct? zip password)
          password
          (recur (rest passwords)))))))


(defn -main [& args]
  (if (or (:help (cli/parse-opts args)) (:h (cli/parse-opts args)))
    (show-help cli-spec)
    (let [{file :file :as opts} (cli/parse-opts args cli-spec)]
      (println "Has zip headers.")
      (if-let [password (matching-password opts file)]
        (println "Password found:" password)
        (println "Password not found.")))))
