# Neil Farbotnik
## Connecting to AWS

```bash
gradle runClient -Phost=<host-ip> -Pport=<port>
```

For example:
```bash
gradle runClient -Phost=54.210.123.45 -Pport=8000
```

## Run things locally without registry

First Terminal
```bash
gradle runNode
```

Second Terminal
```bash
gradle runClient
```
    


### gradle runNode
Will run a node with services. The starter code includes Echo and Joke services as examples. You will need to implement and add the Converter and Library services.

For the Library service: A books.txt file is provided with initial book data (format: title|author|isbn, one per line). Your server should load this on first run and create library_data.json for persistence.

The node registers itself on the Registry. You can change the host and port the node runs on and this will register accordingly with the Registry

### gradle runClient
Will run a client which will call the services from the node, it talks to the node directly not through the registry. At the end the client does some calls to the Registry to pull the services, this will be needed later.


### gradle test
Runs the test cases.

IMPORTANT: Tests expect the server to be running first!
First run in one terminal:
    gradle runNode
Then in second terminal:
    gradle test

The tests connect to localhost:8000 by default.
