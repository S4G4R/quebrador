(ns quebrador.main
  (:require [clojure.string :as string]
            [babashka.fs :as fs]
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


(defn -main [& [file-path]]
  (cond
    (not file-path)
    (prn "You must provide a file path")

    (not (fs/exists? file-path))
    (prn "File does not exist")

    (zip-file? file-path)
    (prn "Has zip headers.")

    :else (prn "Does not have zip headers.")))
