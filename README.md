# Eucalypt
Test assessment: script execution server

## Architecture
### Data flow
``` mermaid
graph LR
    Client -- HTTP POST --> Server

    subgraph Server side
        Server["Eucalypt server <br/> [Kotlin, Ktor]"]

        Server --> Executor1
        Server --> Executor2

        subgraph "Pool of executors"
            subgraph "Container 1"
                Executor1["Executor <br/> [.NET CLI]"]
            end

            subgraph "Container 2"
                Executor2["Executor <br/> [.NET CLI]"]
            end
        end
    end

    Internet((Internet))

    Executor1 -. no access .- Internet
    Executor2 -. no access .- Internet


```

### Contol
``` mermaid
graph LR
    subgraph Eucalypt app
        HTTPHandler["HTTP handler <br/><br/> Serve HTTP requests"]:::Code
        HTTPValidator["HTTP requests validator"]:::Code
        TaskManager["Task runner <br/><br/> - run task <br/> - monitor task state <br/> - handle task results "]:::Code
        PoolManager["Pool manager <br/><br/> - initialize <br/> - monitor state <br/> - extend/shrink <br/> - reserve executor for task"]:::Code
        DockerOperator["Docker operator <br/><br/> Operate: <br/> - Docker images <br/> - containers deployment <br/> - containers monitoring"]:::Code
        
        HTTPHandler -. check request .-> HTTPValidator
        PoolManager -. uses .-> DockerOperator
        TaskManager -. uses .-> PoolManager

        HTTPHandler --> TaskManager
    end

    Pool["Pool of executors <br/><br/> [Docker, .NET CLI]"]:::Pool

    PoolManager -- manage --> Pool
    TaskManager -- run task --> Pool

    classDef Code fill: white, stroke: black
    classDef Pool fill: white, stroke: black, stroke-dasharray: 5 5

```

## Scenarious
### Happy path
``` mermaid
sequenceDiagram
    actor User

    User ->> HTTP Handler: execute sctipt
    activate HTTP Handler
        HTTP Handler ->> HTTP Handler: wrap request to task

        HTTP Handler ->> Task Runner: run task
        activate Task Runner
            Task Runner ->> Pool Manager: get executor
            activate Pool Manager
                Pool Manager ->> Pool Manager: are there any ready executors?
                alt THERE ARE!
                    Pool Manager ->> Pool Manager: reserve executor by ID
                else THERE AREN'T!
                    Pool Manager ->> Pool Manager: register new executor ID

                    Pool Manager ->> Pool: extend pool
                    activate Pool
                        Pool -->> Pool Manager: executor added!
                    deactivate Pool

                    Pool Manager ->> Pool Manager: reserve executor by ID
                end
                
                Pool Manager -->> Task Runner: return reserved executor ID
            deactivate Pool Manager

            Task Runner ->> Pool: execute
            activate Pool
                Pool -->> Task Runner: result
            deactivate Pool

            Task Runner -) Pool Manager: ASYNC return used executor
            
            Task Runner -->> HTTP Handler: task result
            note right of Task Runner: immediately return result
        deactivate Task Runner

        HTTP Handler -->> User: script result
    deactivate HTTP Handler
```

### Timeout of execution
``` mermaid
sequenceDiagram
    actor User

    User ->> HTTP Handler: execute sctipt
    activate HTTP Handler
        HTTP Handler ->> HTTP Handler: wrap request to task

        HTTP Handler ->> Task Runner: run task
        activate Task Runner
            Task Runner ->> Pool Manager: get executor
            activate Pool Manager
                Pool Manager -->> Task Runner: return reserved executor
            deactivate Pool Manager

            Task Runner ->> Pool: execute
            activate Pool
                alt SUCCESS
                    Pool -->> Task Runner: result
                    Task Runner -) Pool Manager: ASYNC return used executor
                else TIMEOUT
                    note right of Task Runner: timeout detected
                    Task Runner -) Pool Manager: ASYNC reset executor
                end
            deactivate Pool

            Task Runner -->> HTTP Handler: task result
            note right of Task Runner: immediately return result
        deactivate Task Runner

        HTTP Handler -->> User: script result
    deactivate HTTP Handler
```

### Pool management processes
Executors have several states:
* NEW – just created; should be checked for readiness
* READY – ready for execution
* RESERVED – locked for specific task; state can be changed only by original task or can be killed by double timeout
* EXECUTING – in the middle of execution; can be killed by timeout
* RELEASED – unlocked by task; should be reset
* RESET – in the middle of reset
* ELIMINATED – marked for elimination; should be destoryed

``` mermaid
stateDiagram-v2
    %% creating
    [*] --> NEW: register new
    NEW --> READY: enable

    %% execution
    READY --> RESERVED: borrow for execution
    RESERVED --> EXECUTING: start execution
    EXECUTING --> RELEASED: success
    RELEASED --> RESET: reset 

    %% execution not started or starvation
    RESERVED --> RESET: double timeout
    EXECUTING --> RESET: timeout
    
    %% reset
    RESET --> READY: enable

    %% elimination
    READY --> ELIMINATED: shrink pool size
    ELIMINATED --> [*]: destroy
```

#### Executor creating
``` mermaid
sequenceDiagram
    activate Pool Manager
        Pool Manager ->> Pool Manager: register executor
        note right of Pool Manager: state of executor is CREATED

        Pool Manager ->> Pool: create container
        activate Pool
            Pool -->> Pool Manager: command executed
        deactivate Pool

        loop poll every N ms
            Pool Manager ->> Pool: check if container in ready state
            activate Pool
        end

        Pool -->> Pool Manager: container is ready!
        deactivate Pool

        Pool Manager ->> Pool Manager: enable executor
        note right of Pool Manager: state of executor is READY

    deactivate Pool Manager
```

#### Release and reset executor
``` mermaid
sequenceDiagram
    note right of Task Runner: ...
    Task Runner -) Pool Manager: ASYNC return used executor
    activate Pool Manager
        Pool Manager ->> Pool Manager: release executor
        note right of Pool Manager: state of executor is RELEASED
    deactivate Pool Manager

    note right of Pool Manager: force to start reset operation
    activate Pool Manager
        Pool Manager ->> Pool Manager: reset executor
        note right of Pool Manager: state of executor is RESET

        Pool Manager ->> Pool: reset executor
        activate Pool
            Pool -->> Pool Manager: command executed
        deactivate Pool

        loop poll every N ms
            Pool Manager ->> Pool: check if container in ready state
            activate Pool
        end

        Pool -->> Pool Manager: container is ready!
        deactivate Pool

        Pool Manager ->> Pool Manager: enable executor for further executions
        note right of Pool Manager: state of executor is READY
    deactivate Pool Manager
```
