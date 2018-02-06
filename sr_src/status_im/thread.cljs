(ns status-im.thread
  (:require [taoensso.timbre :as log]
            [cognitect.transit :as transit]
            [status-im.utils.random :as random]))

(declare init!)

(def Thread (.-Thread (js/require "react-native-threads")))
(defonce thread (atom nil))
(defonce ready? (atom false))
(defonce calls-queue (atom []))

(def reader (transit/reader :json))
(def writer (transit/writer :json))

(defn serialize [o] (transit/write writer o))
(defn deserialize [o] (try (transit/read reader o) (catch :default _ nil)))

(defonce callbacks (atom {}))

(defn post!
  ([message] (post! message nil))
  ([message callback]
   (when-not @thread
     (init!))
   (if @ready?
     (let [id          (random/id)
           message-str (serialize {:id   id
                                   :body message})]
       (swap! callbacks assoc id callback)
       (.postMessage @thread message-str))
     (swap! calls-queue conj [message callback]))))

(defn on-message [message-str]
  (let [{:keys [id] :as message} (deserialize message-str)]
    (log/info "Response for id " id)
    (if (= id :ready)
      (do (reset! ready? true)
          (doseq [[message callback] @calls-queue]
            (post! message callback)
            (reset! calls-queue [])))
      (if (contains? @callbacks id)
        (let [callback (get @callbacks id)]
          (callback message)
          (swap! callbacks dissoc id))
        (log/warn (str "No callback for " id) type)))))

(defn set-on-message! []
  (set! (.-onmessage @thread) on-message))

(defn init! []
  (reset! thread (Thread. "worker.thread.js"))
  (set-on-message!))

(defn terminate! []
  (.terminate @thread)
  (reset! thread nil)
  (reset! ready? false))

(comment
   (post! {:type :ping} println)
   ;;=> nil
   ;;{:id 1519294297196-9cefa313-b7cc-579d-ae0c-fb9f797b31e9, :body :pong}
   (post! {:type :init-realm} println)
   ;;=> nil
   (post! {:type :change-account
           :params {:new-account? false
                    :address "196f2d53e21894ab53944ce1f97ad79ef6fab303"}} println)
   ;;=> nil
   ;;{:id 1519294312479-88496174-988c-5ee8-9799-f85cbbbf0569, :error nil}
   (post! {:type :chats/get-all} println)
   ;;=> nil
   ;;{:id 1519294319033-e87137ec-5f2d-5550-a69b-3d159e466d39, :data ({:updated-at nil, :group-admin nil, :color #a187d5, :contacts [{:identity console, :is-in-chat true}], :last-clock-value 8, :name Console, :removed-from-at nil, :contact-info nil, :is-active true, :debug? false, :added-to-at nil, :group-chat false, :public? false, :removed-at nil, :chat-id console, :timestamp 1519293221970, :unremovable? true, :private-key nil, :public-key nil})}
   )
