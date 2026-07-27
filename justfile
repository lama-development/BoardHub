set shell := ["bash", "-cu"]

compose_file := "docker/docker-compose.yml"
service_dir := "services/event-service"
frontend_dir := "frontend"
formatter := "scripts/format_api_response.py"
base_url := "http://localhost:8082"
venue_key := env_var_or_default("BOARDHUB_VENUE_ADMIN_KEY", "boardhub-local-venue-admin-key")
dm_token := env_var_or_default("BOARDHUB_DM_TOKEN", "")
player_token := env_var_or_default("BOARDHUB_PLAYER_TOKEN", "")
max_tables := env_var_or_default("BOARDHUB_MAX_ACTIVE_TABLES", "8")
check_table := env_var_or_default("BOARDHUB_CHECK_TABLE", "8")

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
    @printf '  just help                       Mostra questa guida\n\n'
    @printf '\033[38;5;39m▲ INFRA\033[0m\n'
    @printf '  just up                         ▲ Avvia PostgreSQL e Mosquitto in Docker\n'
    @printf '  just down                       ▼ Spegne PostgreSQL e Mosquitto senza cancellare i volumi\n'
    @printf '  just ps                         Mostra lo stato dei container del progetto\n'
    @printf '  just db-tables                  Mostra le tabelle presenti nel database BoardHub\n\n'
    @printf '\033[38;5;42m◆ BACKEND\033[0m\n'
    @printf '  just backend                    ◆ Avvia Spring Boot su localhost:8082\n\n'
    @printf '  just frontend                   ◆ Avvia il sito su localhost:5173\n\n'
    @printf '\033[38;5;214m● TEST\033[0m\n'
    @printf '  just test                       ● Esegue i test automatici Maven\n'
    @printf '  just check                      ● Verifica il flusso end-to-end; richiede il backend attivo\n\n'
    @printf '\033[38;5;141m◇ API\033[0m\n'
    @printf '  just health                     ◇ Controlla se il backend e acceso\n'
    @printf '  BOARDHUB_OUTPUT=json just ...   ◇ Mostra la risposta JSON completa e colorata\n'
    @printf '  just qr TAVOLO [HOST]           ◇ Mostra il QR di un tavolo (da 1 a {{ max_tables }})\n'
    @printf '  just venue-tables               ◇ Elenca tutti i tavoli per il personale del locale\n'
    @printf '  just enable-table N [MINUTI]    ◇ Abilita temporaneamente il tavolo N\n'
    @printf '  just disable-table N            ◇ Revoca l avvio su un tavolo non occupato\n'
    @printf '  just venue-close-table N        ◇ Chiude dal locale la sessione del tavolo N\n'
    @printf '  just table-status QR            ◇ Mostra lo stato pubblico del tavolo\n'
    @printf '  just create-session ID          ◇ Crea una sessione D&D con griglia demo\n'
    @printf '  just move-session ID            ◇ Calcola il movimento usando la griglia salvata della sessione\n'
    @printf '  just move-stateless             ◇ Calcola il movimento passando la griglia direttamente nella richiesta\n'
    @printf '  just events ID                  ◇ Legge gli eventi salvati per una sessione\n\n'
    @printf '  just table-session QR           ◇ Risolve il QR nella sessione attiva\n'
    @printf '  just request-join ID PLAYER [NOME]\n'
    @printf '                                  ◇ Invia una richiesta di ingresso al DM\n'
    @printf '  export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'
    @printf '                                  Imposta il token restituito alla creazione\n'
    @printf '  just pending-joins ID           ◇ Elenca le richieste in attesa del DM\n'
    @printf '  just accept-join ID REQUEST     ◇ Accetta una richiesta di ingresso\n'
    @printf '  just reject-join ID REQUEST     ◇ Rifiuta una richiesta di ingresso\n'
    @printf '  just participants ID            ◇ Elenca i partecipanti attivi\n'
    @printf '  just create-character ID        ◇ Crea Elaria per il giocatore autenticato\n'
    @printf '  just my-characters ID           ◇ Elenca i personaggi del giocatore autenticato\n'
    @printf '  just dm-characters ID           ◇ Elenca al DM tutti i personaggi della sessione\n'
    @printf '  just close-session ID           ◇ Conclude la sessione e disabilita il tavolo\n\n'
    @printf '\033[38;5;208m◈ MQTT\033[0m\n'
    @printf '  just publish-event ID [EVENTO]  ◈ Pubblica un evento MOVE sul broker MQTT\n\n'
    @printf '\033[2mEsempio demo:\033[0m\n'
    @printf '  just up\n'
    @printf '  just backend\n'
    @printf '  just enable-table 4\n'
    @printf '  just create-session session-demo-001\n'
    @printf '  export BOARDHUB_DM_TOKEN='\''TOKEN_DM_RICEVUTO'\''\n'
    @printf '  just move-session session-demo-001\n'
    @printf '  just publish-event session-demo-001\n'
    @printf '  just events session-demo-001\n'
    @printf '  just close-session session-demo-001\n'
    @printf '  just down\n'
    @printf '\n\033[2mEsempio ingresso tramite QR:\033[0m\n'
    @printf '  just qr 4\n'
    @printf '  just enable-table 4\n'
    @printf '  just table-status qr-table-04\n'
    @printf '  just request-join session-demo-001 player-device-01 Andrea\n'
    @printf '  just pending-joins session-demo-001\n'
    @printf '  just accept-join session-demo-001 REQUEST_ID\n'
    @printf '  export BOARDHUB_PLAYER_TOKEN='\''TOKEN_RICEVUTO_DA_ACCEPT'\''\n'
    @printf '  just create-character session-demo-001\n'
    @printf '  just my-characters session-demo-001\n'
    @printf '  just dm-characters session-demo-001\n'
    @printf '  just participants session-demo-001\n'

up:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Avvio PostgreSQL e Mosquitto...\n'
    docker compose -f {{ compose_file }} up -d

down:
    @printf '\033[38;5;208m[▼ INFRA]\033[0m Spegnimento container BoardHub...\n'
    docker compose -f {{ compose_file }} down

ps:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Stato container BoardHub:\n'
    docker compose -f {{ compose_file }} ps

db-tables:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Tabelle PostgreSQL nello schema game_schema:\n'
    docker exec boardhub_db psql -U boardhub_user -d boardhub_db -c '\dt game_schema.*'

backend:
    @printf '\033[38;5;42m[◆ BACKEND]\033[0m Avvio event-service su {{ base_url }}...\n'
    cd {{ service_dir }} && mvn spring-boot:run

frontend:
    @printf '\033[38;5;42m[◆ FRONTEND]\033[0m Avvio sito BoardHub su http://localhost:5173...\n'
    npm --prefix {{ frontend_dir }} run dev

test:
    @printf '\033[38;5;214m[● TEST]\033[0m Esecuzione test automatici Maven...\n'
    cd {{ service_dir }} && mvn test

health:
    @printf '\033[38;5;141m[◇ API]\033[0m Health check event-service:\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/actuator/health \
      | python3 {{ formatter }} health

venue-tables:
    @printf '\033[38;5;141m[◇ LOCALE]\033[0m Stato amministrativo dei tavoli:\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/admin/tables \
      -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
      | python3 {{ formatter }} venue-tables

enable-table table_number="1" minutes="10":
    @set -euo pipefail; \
      TABLE_NUMBER="{{ table_number }}"; \
      TABLE_LIMIT="{{ max_tables }}"; \
      case "$TABLE_NUMBER" in ''|*[!0-9]*) printf '\033[1;31m[ERRORE]\033[0m Tavolo non valido: usa un numero da 1 a %s.\n' "$TABLE_LIMIT"; exit 1;; esac; \
      TABLE_NUMBER=$((10#$TABLE_NUMBER)); \
      if [ "$TABLE_NUMBER" -lt 1 ] || [ "$TABLE_NUMBER" -gt "$TABLE_LIMIT" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m I tavoli configurati vanno da 1 a %s.\n' "$TABLE_LIMIT"; exit 1; \
      fi; \
      TABLE_PUBLIC_ID="$(printf 'qr-table-%02d' "$TABLE_NUMBER")"; \
      printf '\033[38;5;141m[◇ LOCALE]\033[0m Abilitazione temporanea di %s...\n' "$TABLE_PUBLIC_ID"; \
      curl --fail-with-body --silent --show-error -X POST "{{ base_url }}/api/v1/admin/tables/$TABLE_PUBLIC_ID/enable" \
        -H 'Content-Type: application/json' \
        -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
        -d '{"durationMinutes":{{ minutes }}}' \
        | python3 {{ formatter }} table-status

disable-table table_number="1":
    @set -euo pipefail; \
      TABLE_NUMBER="{{ table_number }}"; \
      TABLE_LIMIT="{{ max_tables }}"; \
      case "$TABLE_NUMBER" in ''|*[!0-9]*) printf '\033[1;31m[ERRORE]\033[0m Tavolo non valido: usa un numero da 1 a %s.\n' "$TABLE_LIMIT"; exit 1;; esac; \
      TABLE_NUMBER=$((10#$TABLE_NUMBER)); \
      if [ "$TABLE_NUMBER" -lt 1 ] || [ "$TABLE_NUMBER" -gt "$TABLE_LIMIT" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m I tavoli configurati vanno da 1 a %s.\n' "$TABLE_LIMIT"; exit 1; \
      fi; \
      TABLE_PUBLIC_ID="$(printf 'qr-table-%02d' "$TABLE_NUMBER")"; \
      printf '\033[38;5;141m[◇ LOCALE]\033[0m Disabilitazione di %s...\n' "$TABLE_PUBLIC_ID"; \
      curl --fail-with-body --silent --show-error -X POST "{{ base_url }}/api/v1/admin/tables/$TABLE_PUBLIC_ID/disable" \
        -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
        | python3 {{ formatter }} table-status

venue-close-table table_number="1":
    @set -euo pipefail; \
      TABLE_NUMBER="{{ table_number }}"; \
      TABLE_LIMIT="{{ max_tables }}"; \
      case "$TABLE_NUMBER" in ''|*[!0-9]*) printf '\033[1;31m[ERRORE]\033[0m Tavolo non valido: usa un numero da 1 a %s.\n' "$TABLE_LIMIT"; exit 1;; esac; \
      TABLE_NUMBER=$((10#$TABLE_NUMBER)); \
      if [ "$TABLE_NUMBER" -lt 1 ] || [ "$TABLE_NUMBER" -gt "$TABLE_LIMIT" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m I tavoli configurati vanno da 1 a %s.\n' "$TABLE_LIMIT"; exit 1; \
      fi; \
      TABLE_PUBLIC_ID="$(printf 'qr-table-%02d' "$TABLE_NUMBER")"; \
      printf '\033[38;5;141m[◇ LOCALE]\033[0m Chiusura della sessione su %s...\n' "$TABLE_PUBLIC_ID"; \
      curl --fail-with-body --silent --show-error -X POST "{{ base_url }}/api/v1/admin/tables/$TABLE_PUBLIC_ID/close-session" \
        -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
        | python3 {{ formatter }} table-status

qr table_number="1" public_host="":
    @set -euo pipefail; \
      if ! command -v qrencode >/dev/null 2>&1; then \
        printf '\033[1;31m[ERRORE]\033[0m qrencode non e installato.\n'; \
        printf 'Installalo con: \033[1mbrew install qrencode\033[0m\n'; \
        exit 1; \
      fi; \
      TABLE_INPUT="{{ table_number }}"; \
      TABLE_LIMIT="{{ max_tables }}"; \
      case "$TABLE_INPUT" in \
        ''|*[!0-9]*) \
          printf '\033[1;31m[ERRORE]\033[0m Il tavolo deve essere un numero da 1 a %s.\n' "$TABLE_LIMIT"; \
          printf 'Digita, per esempio: \033[1mjust qr 1\033[0m\n'; \
          exit 1 ;; \
      esac; \
      TABLE_NUMBER=$((10#$TABLE_INPUT)); \
      if [ "$TABLE_NUMBER" -lt 1 ] || [ "$TABLE_NUMBER" -gt "$TABLE_LIMIT" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Il tavolo deve essere compreso tra 1 e %s.\n' "$TABLE_LIMIT"; \
        printf 'Scegli un tavolo valido, per esempio: \033[1mjust qr 1\033[0m\n'; \
        exit 1; \
      fi; \
      HOST="{{ public_host }}"; \
      if [ -z "$HOST" ]; then \
        HOST="$(ipconfig getifaddr en0 2>/dev/null || true)"; \
      fi; \
      if [ -z "$HOST" ]; then \
        HOST="$(ipconfig getifaddr en1 2>/dev/null || true)"; \
      fi; \
      if [ -z "$HOST" ]; then \
        HOST="$(ifconfig en0 2>/dev/null | awk '/inet / { print $2; exit }')"; \
      fi; \
      if [ -z "$HOST" ]; then \
        HOST="$(ifconfig en1 2>/dev/null | awk '/inet / { print $2; exit }')"; \
      fi; \
      if [ -z "$HOST" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Indirizzo di rete non rilevato.\n'; \
        printf 'Specificalo manualmente, per esempio: \033[1mjust qr %s 192.168.1.18\033[0m\n' "$TABLE_NUMBER"; \
        exit 1; \
      fi; \
      TABLE_ID="$(printf 'qr-table-%02d' "$TABLE_NUMBER")"; \
      URL="http://$HOST:5173/t/$TABLE_ID"; \
      if ! curl --silent --fail --max-time 2 http://localhost:5173/ >/dev/null; then \
        printf '\033[1;31m[ERRORE]\033[0m Il sito BoardHub non e attivo sulla porta 5173.\n'; \
        printf 'Apri un nuovo terminale nella cartella BoardHub, esegui \033[1mjust frontend\033[0m e lascialo aperto.\n'; \
        exit 1; \
      fi; \
      if ! curl --silent --fail --max-time 2 {{ base_url }}/actuator/health >/dev/null; then \
        printf '\033[1;31m[ERRORE]\033[0m Il backend BoardHub non risponde sulla porta 8082.\n'; \
        printf 'Apri un altro terminale nella cartella BoardHub, esegui \033[1mjust backend\033[0m e lascialo aperto.\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ QR]\033[0m Tavolo %d - %s\n' "$TABLE_NUMBER" "$TABLE_ID"; \
      printf '\033[2m%s\033[0m\n\n' "$URL"; \
      qrencode -t ANSIUTF8 -l M -m 4 "$URL"; \
      printf '\n\033[2mRichiede just frontend attivo e un telefono sulla stessa rete del Mac.\033[0m\n'

create-session session_id table_id="table-04" table_public_id="qr-table-04" table_display_name="Tavolo 4":
    @printf '\033[38;5;141m[◇ API]\033[0m Creazione sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/sessions \
      -H 'Content-Type: application/json' \
      -d '{"sessionId":"{{ session_id }}","venueId":"venue-01","tableId":"{{ table_id }}","tablePublicId":"{{ table_public_id }}","tableDisplayName":"{{ table_display_name }}","title":"Cripta del Re Caduto","gameType":"DND","publicSummary":"Avventura dimostrativa per personaggi di livello 3.","acceptingJoinRequests":true,"grid":{"width":3,"height":3,"difficultCells":["C1"],"blockedCells":["A2"],"obstacleCells":[],"occupiedCells":["A1"],"walls":[{"cell":"B1","direction":"SOUTH"}],"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}' \
      | python3 {{ formatter }} created-session

table-status table_public_id="qr-table-04":
    @printf '\033[38;5;141m[◇ API]\033[0m Stato pubblico di %s...\n' "{{ table_public_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/public/tables/{{ table_public_id }} \
      | python3 {{ formatter }} table-status

table-session table_public_id="qr-table-04":
    @printf '\033[38;5;141m[◇ API]\033[0m Risoluzione QR %s...\n' "{{ table_public_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/public/tables/{{ table_public_id }}/active-session \
      | python3 {{ formatter }} active-session

request-join session_id player_reference="player-device-01" display_name="Giocatore":
    @IDEMPOTENCY_KEY="$(uuidgen | tr '[:upper:]' '[:lower:]')"; \
      printf '\033[38;5;141m[◇ API]\033[0m Richiesta di ingresso per %s...\n' "{{ player_reference }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/public/sessions/{{ session_id }}/join-requests \
        -H 'Content-Type: application/json' \
        -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
        -d '{"playerReference":"{{ player_reference }}","displayName":"{{ display_name }}"}' \
        | python3 {{ formatter }} join-request

pending-joins session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Richieste in attesa per %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests?status=PENDING \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-requests

accept-join session_id request_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Accettazione richiesta %s...\n' "{{ request_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests/{{ request_id }}/accept \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-resolution

reject-join session_id request_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Rifiuto richiesta %s...\n' "{{ request_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests/{{ request_id }}/reject \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-request

participants session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Partecipanti attivi di %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/participants \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} participants

create-character session_id:
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Creazione personaggio demo per %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/player/sessions/{{ session_id }}/characters \
        -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Elaria","species":"Elfa","age":120,"className":"Maga","level":3,"speedCells":6,"hpMax":18,"armorClass":12,"partyVisibility":"OWNER_ONLY"}' \
        | python3 {{ formatter }} character

my-characters session_id:
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Personaggi posseduti nella sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/player/sessions/{{ session_id }}/characters \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} characters

dm-characters session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Personaggi visibili al DM nella sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/characters \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} characters

close-session session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Chiusura sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/close \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} closed-session

move-session session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento sulla sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/sessions/{{ session_id }}/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A1","movementPoints":2}' \
      | python3 {{ formatter }} movement

move-stateless:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento stateless con griglia nel body...\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A3","movementPoints":3,"grid":{"width":6,"height":6,"difficultCells":["B3"],"blockedCells":["D3"],"obstacleCells":["C4"],"occupiedCells":[],"walls":[{"cell":"A3","direction":"EAST"}],"traps":[{"trapId":"trap-01","cell":"B4","visibility":"HIDDEN","armed":true}]}}' \
      | python3 {{ formatter }} movement

publish-event session_id event_id="" table_id="table-04":
    @EVENT_ID="{{ event_id }}"; \
      if [ -z "$EVENT_ID" ]; then EVENT_ID="evt-$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      SEQUENCE_NUMBER="$(date +%s)"; \
      OCCURRED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"; \
      MQTT_TOPIC="boardhub/v1/venues/venue-01/tables/{{ table_id }}/events"; \
      printf '\033[38;5;208m[◈ MQTT]\033[0m Pubblicazione evento %s per sessione %s su %s...\n' "$EVENT_ID" "{{ session_id }}" "$MQTT_TOPIC"; \
      docker exec boardhub_mqtt mosquitto_pub -h localhost -t "$MQTT_TOPIC" \
        -m "{\"eventId\":\"$EVENT_ID\",\"eventType\":\"MOVE\",\"venueId\":\"venue-01\",\"tableId\":\"{{ table_id }}\",\"sessionId\":\"{{ session_id }}\",\"source\":\"SIMULATOR\",\"occurredAt\":\"$OCCURRED_AT\",\"sequenceNumber\":$SEQUENCE_NUMBER,\"payload\":{\"characterId\":\"adv-01\",\"from\":\"A1\",\"to\":\"B1\"}}"

events session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Lettura eventi della sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/sessions/{{ session_id }}/events \
      | python3 {{ formatter }} events

check:
    @printf '\033[38;5;214m[● TEST]\033[0m Verifica rapida del flusso principale...\n'
    @set -euo pipefail; \
      CHECK_SUFFIX="$(date +%Y%m%d%H%M%S)"; \
      SESSION_ID="session-check-$CHECK_SUFFIX"; \
      TABLE_NUMBER="{{ check_table }}"; \
      TABLE_ID="$(printf 'table-%02d' "$TABLE_NUMBER")"; \
      TABLE_PUBLIC_ID="$(printf 'qr-table-%02d' "$TABLE_NUMBER")"; \
      TABLE_DISPLAY_NAME="Tavolo $TABLE_NUMBER"; \
      CREATED=false; \
      cleanup() { if [ "$CREATED" = true ]; then just venue-close-table "$TABLE_NUMBER" >/dev/null 2>&1 || true; fi; }; \
      trap cleanup EXIT; \
      if ! just health; then \
        printf '\033[1;31m[ERRORE]\033[0m Backend non raggiungibile su %s.\n' "{{ base_url }}"; \
        printf 'Apri un secondo terminale, esegui \033[1mjust backend\033[0m e attendi il messaggio di avvio.\n'; \
        exit 1; \
      fi; \
      TABLE_JSON="$(curl --fail-with-body -sS "{{ base_url }}/api/v1/public/tables/$TABLE_PUBLIC_ID")"; \
      TABLE_STATUS="$(node -e 'process.stdout.write(JSON.parse(process.argv[1]).status)' "$TABLE_JSON")"; \
      if [ "$TABLE_STATUS" = "IN_SESSION" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Il tavolo di test %s ospita gia una sessione.\n' "$TABLE_NUMBER"; \
        printf 'Scegli un tavolo senza sessione con: export BOARDHUB_CHECK_TABLE=N\n'; \
        exit 1; \
      fi; \
      printf '\033[2mSessione temporanea: %s sul tavolo %s\033[0m\n' "$SESSION_ID" "$TABLE_NUMBER"; \
      just enable-table "$TABLE_NUMBER" 5 >/dev/null; \
      CREATED_JSON="$(curl --fail-with-body -sS -X POST {{ base_url }}/api/v1/sessions \
        -H 'Content-Type: application/json' \
        -d "{\"sessionId\":\"$SESSION_ID\",\"venueId\":\"venue-01\",\"tableId\":\"$TABLE_ID\",\"tablePublicId\":\"$TABLE_PUBLIC_ID\",\"tableDisplayName\":\"$TABLE_DISPLAY_NAME\",\"title\":\"Verifica automatica BoardHub\",\"gameType\":\"DND\",\"publicSummary\":\"Sessione temporanea di verifica.\",\"acceptingJoinRequests\":true,\"grid\":{\"width\":3,\"height\":3,\"difficultCells\":[\"C1\"],\"blockedCells\":[\"A2\"],\"obstacleCells\":[],\"occupiedCells\":[\"A1\"],\"walls\":[{\"cell\":\"B1\",\"direction\":\"SOUTH\"}],\"traps\":[]}}")"; \
      DM_TOKEN="$(node -e 'process.stdout.write(JSON.parse(process.argv[1]).dmAccessToken)' "$CREATED_JSON")"; \
      if [[ "$DM_TOKEN" != bhd1.* ]]; then \
        printf '\033[1;31m[ERRORE]\033[0m Il backend non ha restituito un token DM valido.\n'; \
        exit 1; \
      fi; \
      CREATED=true; \
      just move-session "$SESSION_ID"; \
      just move-stateless; \
      EVENT_ID="evt-$SESSION_ID"; \
      just publish-event "$SESSION_ID" "$EVENT_ID" "$TABLE_ID"; \
      EVENT_FOUND=false; \
      for ATTEMPT in $(seq 1 10); do \
        EVENTS_JSON="$(curl --fail-with-body -sS "{{ base_url }}/api/v1/sessions/$SESSION_ID/events")"; \
        if command -v jq >/dev/null 2>&1; then \
          if printf '%s' "$EVENTS_JSON" | jq -e --arg event_id "$EVENT_ID" 'any(.[]; .eventId == $event_id)' >/dev/null; then EVENT_FOUND=true; break; fi; \
        elif printf '%s' "$EVENTS_JSON" | grep -Fq "$EVENT_ID"; then \
          EVENT_FOUND=true; break; \
        fi; \
        sleep 0.25; \
      done; \
      if [ "$EVENT_FOUND" != true ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Evento MQTT %s non trovato tra gli eventi persistiti.\n' "$EVENT_ID"; \
        exit 1; \
      fi; \
      printf '\033[38;5;214m[● TEST]\033[0m Evento MQTT ricevuto e persistito: %s\n' "$EVENT_ID"; \
      just events "$SESSION_ID"; \
      BOARDHUB_DM_TOKEN="$DM_TOKEN" just close-session "$SESSION_ID"; \
      CREATED=false
