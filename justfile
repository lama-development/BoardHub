set shell := ["bash", "-cu"]

project_dir := justfile_directory()
compose_file := "docker/docker-compose.yml"
service_dir := "services/event-service"
base_url := "http://localhost:8082"
mqtt_topic := "boardhub/v1/venues/venue-01/tables/table-04/events"

default:
    @just help

help:
    @printf '\033[1;37mBoardHub - comandi rapidi\033[0m\n'
    @printf '\033[2mEsegui questi comandi dalla radice del progetto BoardHub.\033[0m\n\n'
    @printf '\033[1;37mLegenda colori\033[0m\n'
    @printf '  \033[38;5;39m▲ INFRA\033[0m   Docker, PostgreSQL e Mosquitto\n'
    @printf '  \033[38;5;42m◆ BACKEND\033[0m Spring Boot event-service\n'
    @printf '  \033[38;5;214m● TEST\033[0m    Verifiche automatiche o richieste di controllo\n'
    @printf '  \033[38;5;141m◇ API\033[0m     Chiamate REST verso localhost:8082\n'
    @printf '  \033[38;5;208m◈ MQTT\033[0m    Pubblicazione eventi sul broker Mosquitto\n\n'
    @printf '\033[38;5;39m▲ INFRA\033[0m\n'
    @printf '  just up                         ▲ Avvia PostgreSQL e Mosquitto in Docker\n'
    @printf '  just down                       ▼ Spegne PostgreSQL e Mosquitto senza cancellare i volumi\n'
    @printf '  just ps                         Mostra lo stato dei container del progetto\n'
    @printf '  just db-tables                  Mostra le tabelle presenti nel database BoardHub\n\n'
    @printf '\033[38;5;42m◆ BACKEND\033[0m\n'
    @printf '  just backend                    ◆ Avvia Spring Boot su localhost:8082\n\n'
    @printf '\033[38;5;214m● TEST\033[0m\n'
    @printf '  just test                       ● Esegue i test automatici Maven\n'
    @printf '  just check                      ● Esegue una verifica rapida end-to-end\n\n'
    @printf '\033[38;5;141m◇ API\033[0m\n'
    @printf '  just health                     ◇ Controlla se il backend e acceso\n'
    @printf '  just create-session ID          ◇ Crea una sessione D&D con griglia demo\n'
    @printf '  just move-session ID            ◇ Calcola il movimento usando la griglia salvata della sessione\n'
    @printf '  just move-stateless             ◇ Calcola il movimento passando la griglia direttamente nella richiesta\n'
    @printf '  just events ID                  ◇ Legge gli eventi salvati per una sessione\n\n'
    @printf '\033[38;5;208m◈ MQTT\033[0m\n'
    @printf '  just publish-event ID           ◈ Pubblica un evento MOVE sul broker MQTT\n\n'
    @printf '\033[2mEsempio demo:\033[0m\n'
    @printf '  just up\n'
    @printf '  just backend\n'
    @printf '  just create-session session-demo-001\n'
    @printf '  just move-session session-demo-001\n'
    @printf '  just publish-event session-demo-001\n'
    @printf '  just events session-demo-001\n'
    @printf '  just down\n'

up:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Avvio PostgreSQL e Mosquitto...\n'
    docker compose -f {{compose_file}} up -d

down:
    @printf '\033[38;5;208m[▼ INFRA]\033[0m Spegnimento container BoardHub...\n'
    docker compose -f {{compose_file}} down

ps:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Stato container BoardHub:\n'
    docker compose -f {{compose_file}} ps

db-tables:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Tabelle PostgreSQL nello schema game_schema:\n'
    docker exec boardhub_db psql -U boardhub_user -d boardhub_db -c '\dt game_schema.*'

backend:
    @printf '\033[38;5;42m[◆ BACKEND]\033[0m Avvio event-service su {{base_url}}...\n'
    cd {{service_dir}} && mvn spring-boot:run

test:
    @printf '\033[38;5;214m[● TEST]\033[0m Esecuzione test automatici Maven...\n'
    cd {{service_dir}} && mvn test

health:
    @printf '\033[38;5;141m[◇ API]\033[0m Health check event-service:\n'
    curl -i {{base_url}}/actuator/health

create-session session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Creazione sessione %s...\n' "{{session_id}}"
    curl -i -X POST {{base_url}}/api/v1/sessions \
      -H 'Content-Type: application/json' \
      -d '{"sessionId":"{{session_id}}","venueId":"venue-01","tableId":"table-04","title":"Cripta del Re Caduto","gameType":"DND","grid":{"width":3,"height":3,"difficultCells":["C1"],"blockedCells":["A2"],"obstacleCells":[],"occupiedCells":["A1"],"walls":[{"cell":"B1","direction":"SOUTH"}],"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}'

move-session session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento sulla sessione %s...\n' "{{session_id}}"
    curl -i -X POST {{base_url}}/api/v1/sessions/{{session_id}}/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A1","movementPoints":2}'

move-stateless:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento stateless con griglia nel body...\n'
    curl -i -X POST {{base_url}}/api/v1/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A3","movementPoints":3,"grid":{"width":6,"height":6,"difficultCells":["B3"],"blockedCells":["D3"],"obstacleCells":["C4"],"occupiedCells":[],"walls":[{"cell":"A3","direction":"EAST"}],"traps":[{"trapId":"trap-01","cell":"B4","visibility":"HIDDEN","armed":true}]}}'

publish-event session_id event_id="evt-test-manuale-001":
    @printf '\033[38;5;208m[◈ MQTT]\033[0m Pubblicazione evento %s per sessione %s...\n' "{{event_id}}" "{{session_id}}"
    docker exec boardhub_mqtt mosquitto_pub -h localhost -t "{{mqtt_topic}}" -m '{"eventId":"{{event_id}}","eventType":"MOVE","venueId":"venue-01","tableId":"table-04","sessionId":"{{session_id}}","source":"SIMULATOR","occurredAt":"2026-07-07T10:00:00Z","sequenceNumber":1,"payload":{"characterId":"adv-01","from":"A1","to":"B1"}}'

events session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Lettura eventi della sessione %s...\n' "{{session_id}}"
    curl -i {{base_url}}/api/v1/sessions/{{session_id}}/events

check:
    @printf '\033[38;5;214m[● TEST]\033[0m Verifica rapida del flusso principale...\n'
    @SESSION_ID="session-check-$$(date +%Y%m%d%H%M%S)"; \
      printf '\033[2mSessione temporanea: %s\033[0m\n' "$$SESSION_ID"; \
      just health; \
      just create-session "$$SESSION_ID"; \
      just move-session "$$SESSION_ID"; \
      just move-stateless; \
      just publish-event "$$SESSION_ID" "evt-$$SESSION_ID-001"; \
      sleep 1; \
      just events "$$SESSION_ID"
