(ns status-im.data-store-worker.local-storage
  (:require [status-im.data-store-worker.realm.local-storage :as data-store]))


(defn get-data [chat-id]
  (:data (data-store/get-by-chat-id chat-id)))

(defn set-data [local-storage]
  (data-store/save local-storage))
