# Neil Farbotnik
## Protocol Description
This system uses Proto for its protocol to communicate between a Leader and N Workers.

- `Request`: sent by the Leader to Workers. 
  - Contains num1, num2 (operands)
  - type: (operation: ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, MODULAR, CONSENSUS)
  - message (worker ID or total worker count)
- `Response`: sent by Workers back to the Leader. 
  - Contains the result.

**How they interact:**

1. The Leader connects to each Worker on consecutive ports starting at 9099.
2. For each task, the Leader sends a Request to every Worker with the operation and operands. The worker's ID is sent in the message field.
3. Each Worker computes the result, sends back a Response, and waits.
4. The Leader collects responses for up to 20 seconds, then runs consensus. 
5. A value wins if more than half of all workers agree on it. If no single answer has enough votes, whoever got the most votes still wins. If it's a perfect tie with no clear winner, the round is skipped and the vote breakdown is shown. 
6. The Leader sends a CONSENSUS type Request to all Workers announcing the final result. This request carries the agreed value in num1, the agreed count in num2, and total workers in message. 
7. Workers receive the consensus message, print the result, check if they voted with the majority, then wait for the next task.

## Generating The Proto Code
Run:

```bash
gradle generateProto
```

## Starting the Workers
Start each Worker in its own terminal. The port is done automatically from the ID number (Worker1 = 9099, Worker2 = 9100, etc.):

```bash
gradle runWorker -Pid=Worker1
gradle runWorker -Pid=Worker2
gradle runWorker -Pid=Worker3
gradle runWorker -Pid=Worker4
gradle runWorker -Pid=Worker5
```

## Starting the Leader
Then start the Leader in a separate terminal, specifying how many workers you started:

```bash
gradle runLeader -Pworkers=5
```

- host and port default to `localhost` and `9099` 

## Worker Failure Handling

**Connection failures** : if a worker is unreachable at startup, its socket stays null and the leader skips it when sending tasks.

**Response failures** : each worker response is collected on its own thread. If a worker crashes or times out, it is excluded from the vote count.

**10 second timeout** : the leader waits at most 20 seconds for all responses, then proceeds with whatever has come in.

**Consensus** : a result only wins if it receives votes from strictly more than half of all workers.

## Tie Handling
If no strict majority is reached, the most common value among responding workers is used. If votes are evenly split with no clear winner, the round is skipped and the vote distribution is printed:

```
No consensus reached
Vote distribution: 42 (2 votes), 43 (2 votes)
```

## Issues Encountered

**Threading the leader to connect to multiple workers**:
The first major hurdle was wrapping my head around the model of the leader since it was so different from the previous assignment. 
Normally you think of the terminal doing calculations as the server, which is why I decided to implement it this way. 
It was more challenging because I wasn't used to connecting to many servers simulataneously, and it''s usually the other way around.
The initial approach just looped and connected sequentially, which worked but meant the connections happened one at a time. 
The fix was spawning one thread per worker connection using the runnable pattern, storing each socket in a shared array indexed by worker ID, 
then calling join() on all threads. Once that pattern clicked. The rest of the connection logic fell into place.

## Edge Cases and Limitations

**1. Division by zero** :
If the user enters a task like 5 / 0, the worker throws an ArithmeticException inside run().
**2. Even number of workers** :
With an even number of workers the majority threshold is N/2 + 1, which means all workers essentially need to agree. 
For example with 2 workers, majority is 2. If they ever return different results, neither hits majority AND the fallback also ties at 1 vs 1,
so the round is always skipped with no result. Odd numbers of workers are much safer since a single disagreeing worker cannot block consensus.
**3. Input formatting** :
The leader expects input in the exact format number operator number with a space between each token (e.g. 3 + 5). Inputs without spaces like 3+5 will be rejected. 


