(ns status-im.worker
  (:require [cognitect.transit :as transit]
            [status-im.data-store-worker.core :as data-store]
            [status-im.data-store-worker.chats :as chats]
            [taoensso.timbre :as log]))

(def self (.-self (js/require "react-native-threads")))

(def reader (transit/reader :json))
(def writer (transit/writer :json))

(defn serialize [o] (transit/write writer o))
(defn deserialize [o] (try (transit/read reader o) (catch :default _ nil)))

(defn post! [message]
  (.postMessage self (serialize message)))

(defn on-message [message-str]
  (let [{:keys [id body]} (deserialize message-str)
        {:keys [type]} body]
    (log/info "Received message " type)
    (case type
      :ping (post! {:id id :body :pong})
      :init-realm (data-store/do-init)
      :change-account (let [handler (fn [error]
                                      (post! {:id    id
                                              :error error}))
                            params  (assoc (:params body) :handler handler)]
                        (data-store/do-change-account params))
      :chats/get-all (let [data (chats/get-all)]
                       (post! {:id   id
                               :data data})))))

(set! (.-onmessage self) on-message)

(post! {:id :ready})
