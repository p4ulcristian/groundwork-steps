# groundwork-steps

A handler's work as a list of steps over one map, the context, and the
shape of the answer the steps end with: what the groundwork libraries
share, so none of them needs another to read or make it. A step is
`{:name :enter}`; the steps run in order until one adds an answer, data or
a refusal. One file for both sides, `groundwork-steps.api` (`.cljc`), with
no dependencies.

```clojure
io.github.p4ulcristian/groundwork-steps {:git/tag "v0.1.0" :git/sha "SHA"}
```

## The api

| name | does |
|---|---|
| `step-shape` | a step as a malli shape: `{:name a-keyword :enter (fn [context] context)}` |
| `run-steps-until-answered` | runs each step's `:enter` on the context, in order, until one adds an answer; the last context |
| `add-answer-to-context` `does-context-have-answer?` | the answer, at `:groundwork-steps/answer`; whether one is there |
| `make-refusal` `refuse` | a refusal, `{:groundwork-steps/refused reason :groundwork-steps/data data}`; a step answering with one |
| `is-error-answer?` | a refusal rather than data: a map carrying `:groundwork-steps/refused` |
| `refusals` | the reasons every answer may carry besides its handler's own, with their data's shapes |

The context, as a message's runner (groundwork-bridge) puts it together
before the first step, and what the steps add:

| key | holds |
|---|---|
| `:groundwork-steps/request` | the request the message came with (a Ring request), nil for none (a REPL's) |
| `:groundwork-steps/message-name` `:groundwork-steps/message-data` | the message's name and its data, shaped |
| `:groundwork-steps/log` | the app's log, with the message's request id; a step that learns who asks puts a log with more context back |
| `:groundwork-steps/shaper` | the app's shaper |
| `:groundwork-steps/answer` | the answer a step added, data or a refusal |
| `:groundwork-steps/notes` | notes about the answer, besides it (the tables it was read from); they travel beside the answers, under `:groundwork-steps/notes` by name |

A refusal leaves with the message's request id:

```clojure
{:orders/ship {:groundwork-steps/refused    :order/already-shipped
               :groundwork-steps/data       {:order/shipped-at "…"}
               :groundwork-steps/request-id "5f0c…"}}
```

The reason is the key of its text in the app's catalogs, and its data the
text's arguments. `refusals` are the five reasons that are no handler's
own: `:groundwork-steps/too-many {:retry-after-ms n}`,
`:groundwork-steps/failed` (a bug; its details are in the log under the
request id), `:groundwork-steps/invalid {:errors …}`, and the browser's
`:groundwork-steps/no-answer {:http-status n}` and
`:groundwork-steps/unfit {:errors …}`.

```clojure
(require '[groundwork-steps.api :as steps])

(def find-orders-of-shop
  {:name  :find-orders-of-shop
   :enter (fn [{:groundwork-steps/keys [log message-data] :as context}]
            (log :debug :orders/finding {:shop/id (:shop/id message-data)})
            (if-let [orders (find-orders db (:shop/id message-data))]
              (steps/add-answer-to-context context orders)
              (steps/refuse context :shop/closed nil)))})

(steps/run-steps-until-answered [find-orders-of-shop] context)
(steps/is-error-answer? answer)   ; a refusal, or data
```

Nothing here sets behaviour, so there are no settings.

## Tests

```sh
clojure -X:test
```

Why it is the way it is: [`decisions/`](decisions/README.md).

MIT license.
