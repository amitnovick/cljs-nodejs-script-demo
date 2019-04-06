(ns demo.script
  (:require [clojure.repl :refer [doc]]
            ["fuse-bindings" :as fuse]))

(defn main [& cli-args]
  (prn "hello world"))

;; Note: not working
;(defn start [] ; dev/after-load
;  (js/console.log "dev: after load"))
;
;; Note: not working
;(defn stop [done] ; dev/before-load
;  (js/console.log "dev: before load")
;  (done))

(def mount-path "./mnt")

(defn read-dir
  [path, cb]
  (js/console.log "readdir(%s)" path)
  (if (= path "/") (cb 0 (clj->js ["test" "testing"])) (cb 0)))

;; Testing read-dir
;(defn on-read-dir
;  [result-code files]
;  (println "Got:" result-code files))

;(read-dir "/" on-read-dir) ; => Got: 0 [test]

(defn get-attr
  [path cb]
  (js/console.log "getattr(%s)" path)
  (if (= path "/")
    (let [root-attr-map {
                         :mtime (js/Date)
                         :atime (js/Date)
                         :ctime (js/Date)
                         :nlink 1
                         :size  100
                         :mode  16877
                         :uid   (if (nil? (process.getuid)) 0 (process.getuid))
                         :gid   (if (nil? (process.getuid)) 0 (process.getuid))
                         }
          ]
      (cb 0 (clj->js root-attr-map))
      nil)                                                  ; Not sure if necessary
    (if (= path "/test")
      (let [test-attr-map {
                           :mtime (js/Date)
                           :atime (js/Date)
                           :ctime (js/Date)
                           :nlink 1
                           :size  12
                           :mode  33188
                           :uid   (if (nil? (process.getuid)) 0 (process.getuid))
                           :gid   (if (nil? (process.getuid)) 0 (process.getuid))
                           }]
        (cb 0 (clj->js test-attr-map))
        nil)                                                ; Not sure if necessary
      (cb (.-ENOENT fuse))
      )))

(defn open
  [path flags cb]
  (js/console.log "open(%s, %d)" path flags)
  (cb 0 42))                                                ; 42 is fd

(defn read-helper
  [buf cb str]
  (.write buf str)
  (cb (.-length str)))

(defn read
  [path fd buf len pos cb]
  (js/console.log "read(%s, %d, %d, %d)" path fd len pos)
  (let [str (.slice "hello world\n" pos (+ pos len))]
    (if (= str "") (cb 0)
                   (read-helper buf cb str))))

(def operators {:readdir read-dir
                :getattr get-attr
                :open    open
                :read    read})

(defn on-mount-error
  [error]
  (if (not= error nil) (throw error))
  (js/console.log (str "filesystem mounted on " mount-path)))

(defn on-unmount-error
  [error]
  (if (not= error nil)
    (js/console.log (str "filesystem at " mount-path " not unmounted ") (.-message error))
    (js/console.log (str "filesystem at " mount-path " unmounted"))))

(defn on-signal-interruption
  []
  ((.-unmount fuse) mount-path on-unmount-error))

(defn mount!
  []
  ((.-mount fuse) mount-path (clj->js operators) on-mount-error)
  (process.on "SIGINT" on-signal-interruption))

(defn unmount!
  []
  ((.-unmount fuse) mount-path on-unmount-error))