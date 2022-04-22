# network-explorer
Explore metrics about the Urbit network. UI served at https://network.urbit.org, backend served at https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/.

## endpoints
All methods are GET requests and can be tested through a browser.

`/get-node`, [`https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-node?urbit-id=~dinleb-rambep`](https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-node?urbit-id=~dinleb-rambep)

`/get-nodes`, [`https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-nodes?limit=10&offset=10`](https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-nodes?limit=10&offset=10)

`/get-pki-events`, [`https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-pki-events?limit=10&offset=10`](https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-pki-events?limit=10&offset=10)

`/get-aggregate-status`, [`https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-aggregate-status?=&since=2022-01-01&until=2022-01-10`](https://mt2aga2c5l.execute-api.us-east-2.amazonaws.com/get-aggregate-status?=&since=2022-01-01&until=2022-01-10)

## backend architecture diagram
![architecture diagram](./architecture.svg)
