(ns status-im.chat.core)

;; Seen messages
(defn receive-seen
  [chat-id sender message-ids {:keys [db]}]
  (when-let [seen-messages-ids (-> (get-in db [:chats chat-id :messages])
                                   (select-keys message-ids)
                                   keys)]
    {:db (reduce (fn [new-db message-id]
                   (assoc-in new-db [:chats chat-id :messages message-id :user-statuses sender] :seen))
                 db
                 seen-messages-ids)}))

;; Group chat
(defn participants-added [chat-id added-participants-set {:keys [db] :as cofx}]
  (when (seq added-participants-set)
    {:db (update-in db [:chats chat-id :contacts] concat (mapv #(hash-map :identity %) added-participants-set))
     :data-store/add-chat-contacts [chat-id added-participants-set]}))

(defn participants-removed [chat-id removed-participants-set {:keys [now db] :as cofx}]
  (when (seq removed-participants-set)
    (let [{:keys [is-active timestamp]} (get-in db [:chats chat-id])]
      ;;TODO: not sure what this condition is for
      (when (and is-active (>= now timestamp))
        {:db (update-in db [:chats chat-id :contacts]
                        (partial remove (comp removed-participants-set :identity)))
         :data-store/remove-chat-contacts [chat-id removed-participants-set]}))))
