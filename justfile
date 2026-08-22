set shell := ["bash", "-cu"]

compose_file := "docker/docker-compose.yml"
service_dir := "services/event-service"
frontend_dir := "frontend"
formatter := "scripts/format_api_response.py"
base_url := "http://localhost:8082"
stats_url := "http://localhost:8083"
stats_dir := "services/stats-service"
venue_key := env_var_or_default("BOARDHUB_VENUE_ADMIN_KEY", "boardhub-local-venue-admin-key")
dm_token := env_var_or_default("BOARDHUB_DM_TOKEN", "")
player_token := env_var_or_default("BOARDHUB_PLAYER_TOKEN", "")
max_tables := env_var_or_default("BOARDHUB_MAX_ACTIVE_TABLES", "8")
check_table := env_var_or_default("BOARDHUB_CHECK_TABLE", "8")
edge_dir := "edge"
edge_python := "edge/.venv/bin/python"

# Mostra la guida dei comandi disponibili
default:
    @just help

# Mostra la guida completa dei comandi
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
    @printf '\033[38;5;42m◆ STATISTICHE\033[0m\n'
    @printf '  just stats                      ◆ Avvia il servizio statistiche su localhost:8083\n'
    @printf '  just stats-health               ◇ Controlla se il servizio statistiche e acceso\n'
    @printf '  just stats-sessions             ◇ Elenca le sessioni concluse\n'
    @printf '  just stats-session ID           ◇ Risultato di una sessione conclusa\n'
    @printf '  just stats-player RIFERIMENTO   ◇ Statistiche complessive di un giocatore\n'
    @printf '  just create-tournament NOME     ◇ Crea un torneo sulle sessioni gia concluse\n'
    @printf '  just tournaments                ◇ Elenca i tornei\n'
    @printf '  just leaderboard ID             ◇ Classifica finale del torneo\n'
    @printf '  just stats-test                 ● Test del servizio statistiche\n\n'
    @printf '\033[38;5;208m◈ EDGE\033[0m\n'
    @printf '  just edge-setup                 ◈ Prepara l ambiente Python del nodo edge\n'
    @printf '  just edge                       ◈ Avvia il nodo edge con coda locale persistente\n'
    @printf '  just edge-observe SESSIONE      ◈ Accoda un percorso osservato, anche a broker spento\n'
    @printf '  just edge-queue                 ◈ Mostra il contenuto della coda locale\n'
    @printf '  just edge-test                  ● Esegue i test dei componenti del nodo edge\n'
    @printf '  just edge-status                ◈ Mostra lo stato tecnico del nodo edge\n'
    @printf '  just measure [CAMPIONI] [TAVOLO]\n'
    @printf '                                  ● Misura latenza online e recupero offline\n'
    @printf '  just check-offline SESSIONE     ● Scenario completo di disconnessione e recupero\n\n'
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
    @printf '  just close-all                  ◇ Chiude tutte le partite e libera tutti i tavoli\n'
    @printf '  just table-status QR            ◇ Mostra lo stato pubblico del tavolo\n'
    @printf '  just create-session ID          ◇ Crea una sessione D&D con griglia demo\n'
    @printf '  just create-trap-session ID     ◇ Crea una demo con trappola obbligata in C2\n'
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
    @printf '  just create-piece ID CHAR [CELLA]\n'
    @printf '                                  ◇ Posiziona la pedina virtuale del personaggio\n'
    @printf '  just my-pieces ID               ◇ Elenca le pedine del giocatore autenticato\n'
    @printf '  just dm-pieces ID               ◇ Elenca al DM tutte le pedine della sessione\n'
    @printf '  just piece-reachable ID PIECE   ◇ Mostra dove puo arrivare la pedina\n'
    @printf '  just move-piece ID PIECE CELLA [VERSIONE] [COMMAND]\n'
    @printf '                                  ◇ Conferma uno spostamento autorevole\n'
    @printf '  just trap-status ID RESOLUTION  ◇ Mostra la trappola in attesa del giocatore\n'
    @printf '  just roll-trap ID RESOLUTION VERSIONE\n'
    @printf '                                  ◇ Esegue il tiro salvezza della trappola\n'
    @printf '  just continue-trap ID RESOLUTION VERSIONE\n'
    @printf '                                  ◇ Prosegue il percorso dopo la risoluzione\n'
    @printf '  just pending-traps ID           ◇ Elenca al DM le trappole da risolvere\n'
    @printf '  just assume-character ID CHAR   ◇ Il DM assume temporaneamente il controllo\n'
    @printf '  just release-character ID CHAR  ◇ Il DM restituisce il controllo al giocatore\n'
    @printf '  just dm-move-piece ID PIECE CELLA [VERSIONE] [COMMAND]\n'
    @printf '                                  ◇ Muove una pedina controllata dal DM\n'
    @printf '  just dm-piece-reachable ID PIECE\n'
    @printf '                                  ◇ Mostra le celle raggiungibili al DM\n'
    @printf '  just dm-roll-trap ID RESOLUTION VERSIONE\n'
    @printf '                                  ◇ Il DM esegue il tiro salvezza\n'
    @printf '  just dm-continue-trap ID RESOLUTION VERSIONE\n'
    @printf '                                  ◇ Il DM prosegue il percorso risolto\n'
    @printf '  just player-events ID           ◇ Eventi filtrati del giocatore autenticato\n'
    @printf '  just dm-events ID               ◇ Eventi completi visibili al DM\n'
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
    @printf '  just create-piece session-demo-001 CHARACTER_ID B2\n'
    @printf '  just my-pieces session-demo-001\n'
    @printf '  just piece-reachable session-demo-001 PIECE_ID\n'
    @printf '  just move-piece session-demo-001 PIECE_ID C2 0\n'
    @printf '  just trap-status session-demo-001 RESOLUTION_ID\n'
    @printf '  just roll-trap session-demo-001 RESOLUTION_ID VERSIONE\n'
    @printf '  just continue-trap session-demo-001 RESOLUTION_ID VERSIONE\n'
    @printf '  just dm-pieces session-demo-001\n'
    @printf '  just participants session-demo-001\n'

# Prepara l ambiente Python del nodo edge
edge-setup:
    @printf '\033[38;5;208m[◈ EDGE]\033[0m Preparazione ambiente Python del nodo edge...\n'
    python3 -m venv {{ edge_dir }}/.venv
    {{ edge_dir }}/.venv/bin/pip install --quiet --upgrade pip
    {{ edge_dir }}/.venv/bin/pip install --quiet -r {{ edge_dir }}/requirements.txt
    @printf 'Ambiente pronto. Avvia il nodo con: just edge\n'

# Avvia il nodo edge con coda locale persistente
edge:
    @printf '\033[38;5;208m[◈ EDGE]\033[0m Avvio nodo edge; interrompi con Ctrl+C.\n'
    cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge run

# Accoda osservazioni della plancia, anche a broker spento
edge-observe session_id character_id="adv-01":
    @printf '\033[38;5;208m[◈ EDGE]\033[0m Osservazioni accodate localmente prima di ogni invio.\n'
    cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge simulate --session {{ session_id }} --character {{ character_id }}

# Esegue i test dei componenti del nodo edge
edge-test:
    @printf '\033[38;5;214m[● TEST]\033[0m Test dei componenti del nodo edge...\n'
    cd {{ edge_dir }} && .venv/bin/python -m unittest discover -s tests -v

# Misura latenza online e recupero offline con campioni grezzi e report
measure samples="10" table_number="7":
    @printf '\033[38;5;214m[● TEST]\033[0m Baseline prestazioni online e recupero offline...\n'
    @if [ ! -x "{{ edge_python }}" ]; then \
      printf '\033[1;31m[ERRORE]\033[0m Ambiente edge assente. Esegui prima: just edge-setup\n'; \
      exit 1; \
    fi
    cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge.measurement \
      --samples {{ samples }} --table {{ table_number }} \
      --output ../artifacts/measurements

# Mostra il contenuto della coda locale del nodo edge
edge-queue:
    cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge list

# Mostra lo stato tecnico del nodo edge
edge-status:
    cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge status

# Scenario completo di disconnessione dalla rete e recupero degli eventi
check-offline session_id="session-offline-demo-001":
    @printf '\033[38;5;214m[● TEST]\033[0m Scenario di disconnessione e recupero...\n'
    @set -euo pipefail; \
      SESSION_ID="{{ session_id }}"; \
      if ! just health >/dev/null 2>&1; then \
        printf '\033[1;31m[ERRORE]\033[0m Backend non raggiungibile. Esegui prima: just backend\n'; \
        exit 1; \
      fi; \
      if [ ! -x "{{ edge_python }}" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Ambiente edge assente. Esegui prima: just edge-setup\n'; \
        exit 1; \
      fi; \
      printf '\033[2m1/6 Spegnimento del broker MQTT\033[0m\n'; \
      docker compose -f {{ compose_file }} stop mosquitto >/dev/null 2>&1; \
      printf '\033[2m2/6 Il sensore osserva la partita mentre la rete e assente\033[0m\n'; \
      just edge-observe "$SESSION_ID" >/dev/null; \
      just edge-queue; \
      printf '\033[2m3/6 Riaccensione del broker MQTT\033[0m\n'; \
      docker compose -f {{ compose_file }} start mosquitto >/dev/null 2>&1; \
      sleep 4; \
      printf '\033[2m4/6 Il nodo edge riconsegna la coda\033[0m\n'; \
      (cd {{ edge_dir }} && .venv/bin/python -m boardhub_edge run >/dev/null 2>&1 &) ; \
      sleep 12; \
      pkill -f "boardhub_edge run" >/dev/null 2>&1 || true; \
      printf '\033[2m5/6 Stato finale della coda locale\033[0m\n'; \
      just edge-queue; \
      printf '\033[2m6/6 Eventi persistiti nel backend\033[0m\n'; \
      just events "$SESSION_ID"

# Avvia il servizio statistiche su localhost:8083
stats:
    @printf '\033[38;5;42m[◆ STATISTICHE]\033[0m Avvio del servizio statistiche su {{ stats_url }}...\n'
    cd {{ stats_dir }} && mvn spring-boot:run

# Esegue i test del servizio statistiche
stats-test:
    @printf '\033[38;5;214m[● TEST]\033[0m Test del servizio statistiche...\n'
    cd {{ stats_dir }} && mvn test

# Controlla se il servizio statistiche e acceso
stats-health:
    @curl --fail-with-body -sS {{ stats_url }}/actuator/health | python3 {{ formatter }} stats-health

# Elenca le partite concluse
stats-sessions:
    @printf '\033[38;5;141m[◇ API]\033[0m Sessioni concluse...\n'
    @curl --fail-with-body -sS {{ stats_url }}/api/v1/stats/sessions | python3 {{ formatter }} session-results

# Mostra il risultato di una partita conclusa
stats-session session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Risultato della sessione {{ session_id }}...\n'
    @curl --fail-with-body -sS {{ stats_url }}/api/v1/stats/sessions/{{ session_id }} | python3 {{ formatter }} session-result

# Mostra le statistiche complessive di un giocatore
stats-player player_reference:
    @printf '\033[38;5;141m[◇ API]\033[0m Statistiche di {{ player_reference }}...\n'
    @curl --fail-with-body -sS {{ stats_url }}/api/v1/stats/players/{{ player_reference }} | python3 {{ formatter }} player-statistics

# Crea un torneo sulle partite gia concluse
create-tournament name="Coppa del Locale" game_type="DND" venue_id="venue-01":
    @printf '\033[38;5;141m[◇ API]\033[0m Creazione torneo...\n'
    @curl --fail-with-body -sS -X POST {{ stats_url }}/api/v1/tournaments \
      -H 'Content-Type: application/json' \
      -d '{"name":"{{ name }}","gameType":"{{ game_type }}","venueId":"{{ venue_id }}"}' \
      | python3 {{ formatter }} tournament

# Elenca i tornei creati
tournaments:
    @curl --fail-with-body -sS {{ stats_url }}/api/v1/tournaments | python3 {{ formatter }} tournaments

# Mostra la classifica finale di un torneo
leaderboard tournament_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Classifica del torneo...\n'
    @curl --fail-with-body -sS {{ stats_url }}/api/v1/tournaments/{{ tournament_id }}/leaderboard | python3 {{ formatter }} leaderboard

# ATTENZIONE: chiude TUTTE le partite in corso e libera tutti i tavoli del locale
close-all:
    @printf '\033[38;5;141m[◇ LOCALE]\033[0m Chiusura di tutte le partite e liberazione dei tavoli...\n'
    @set -euo pipefail; \
      if ! curl -sf {{ base_url }}/actuator/health >/dev/null 2>&1; then \
        printf '\033[1;31m[ERRORE]\033[0m Backend non raggiungibile. Esegui prima: just backend\n'; \
        exit 1; \
      fi; \
      TABLES="$(curl --fail-with-body -sS -H 'X-BoardHub-Venue-Key: {{ venue_key }}' {{ base_url }}/api/v1/admin/tables)"; \
      CHIUSE=0; REVOCATE=0; \
      for ROW in $(printf '%s' "$TABLES" | python3 -c "import json,sys; print(' '.join(f\"{t['tablePublicId']}:{t['status']}\" for t in json.load(sys.stdin)))"); do \
        QR="${ROW%%:*}"; STATO="${ROW##*:}"; \
        if [ "$STATO" = "IN_SESSION" ]; then \
          curl --fail-with-body -sS -X POST -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
            "{{ base_url }}/api/v1/admin/tables/$QR/close-session" >/dev/null; \
          printf '  partita chiusa      %s\n' "$QR"; \
          CHIUSE=$((CHIUSE+1)); \
        elif [ "$STATO" = "CLAIMABLE" ]; then \
          curl --fail-with-body -sS -X POST -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
            "{{ base_url }}/api/v1/admin/tables/$QR/disable" >/dev/null; \
          printf '  abilitazione revocata  %s\n' "$QR"; \
          REVOCATE=$((REVOCATE+1)); \
        fi; \
      done; \
      printf '\n  Partite chiuse: %s   Abilitazioni revocate: %s\n' "$CHIUSE" "$REVOCATE"; \
      printf '\033[2m  Tutti i tavoli sono ora liberi.\033[0m\n'
    @just venue-tables

# Avvia PostgreSQL e Mosquitto in Docker
up:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Avvio PostgreSQL e Mosquitto...\n'
    docker compose -f {{ compose_file }} up -d

# Spegne PostgreSQL e Mosquitto senza cancellare i dati
down:
    @printf '\033[38;5;208m[▼ INFRA]\033[0m Spegnimento container BoardHub...\n'
    docker compose -f {{ compose_file }} down

# Mostra lo stato dei container del progetto
ps:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Stato container BoardHub:\n'
    docker compose -f {{ compose_file }} ps

# Elenca le tabelle presenti nel database
db-tables:
    @printf '\033[38;5;39m[▲ INFRA]\033[0m Tabelle PostgreSQL nello schema game_schema:\n'
    docker exec boardhub_db psql -U boardhub_user -d boardhub_db -c '\dt game_schema.*'

# Avvia il servizio di gioco su localhost:8082
backend:
    @printf '\033[38;5;42m[◆ BACKEND]\033[0m Avvio event-service su {{ base_url }}...\n'
    cd {{ service_dir }} && mvn spring-boot:run

# Avvia il sito su localhost:5173
frontend:
    @printf '\033[38;5;42m[◆ FRONTEND]\033[0m Avvio sito BoardHub su http://localhost:5173...\n'
    npm --prefix {{ frontend_dir }} run dev

# Esegue i test automatici del servizio di gioco
test:
    @printf '\033[38;5;214m[● TEST]\033[0m Esecuzione test automatici Maven...\n'
    cd {{ service_dir }} && mvn test

# Controlla se il servizio di gioco e acceso
health:
    @printf '\033[38;5;141m[◇ API]\033[0m Health check event-service:\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/actuator/health \
      | python3 {{ formatter }} health

# Elenca tutti i tavoli per il personale del locale
venue-tables:
    @printf '\033[38;5;141m[◇ LOCALE]\033[0m Stato amministrativo dei tavoli:\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/admin/tables \
      -H 'X-BoardHub-Venue-Key: {{ venue_key }}' \
      | python3 {{ formatter }} venue-tables

# Abilita temporaneamente un tavolo per avviare una partita
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

# Revoca l abilitazione di un tavolo non occupato
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

# Chiude dal locale la partita di un singolo tavolo
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

# Mostra il QR da inquadrare per un tavolo
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

# Crea una partita con griglia dimostrativa
create-session session_id table_id="table-04" table_public_id="qr-table-04" table_display_name="Tavolo 4":
    @printf '\033[38;5;141m[◇ API]\033[0m Creazione sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/sessions \
      -H 'Content-Type: application/json' \
      -d '{"sessionId":"{{ session_id }}","venueId":"venue-01","tableId":"{{ table_id }}","tablePublicId":"{{ table_public_id }}","tableDisplayName":"{{ table_display_name }}","title":"Cripta del Re Caduto","gameType":"DND","publicSummary":"Avventura dimostrativa per personaggi di livello 3.","acceptingJoinRequests":true,"grid":{"width":3,"height":3,"difficultCells":["C1"],"blockedCells":["A2"],"obstacleCells":[],"occupiedCells":["A1"],"walls":[{"cell":"B1","direction":"SOUTH"}],"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}' \
      | python3 {{ formatter }} created-session

# Crea una partita con trappola obbligata in C2
create-trap-session session_id table_id="table-08" table_public_id="qr-table-08" table_display_name="Tavolo 8":
    @printf '\033[38;5;141m[◇ API]\033[0m Creazione demo trappola %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/sessions \
      -H 'Content-Type: application/json' \
      -d '{"sessionId":"{{ session_id }}","venueId":"venue-01","tableId":"{{ table_id }}","tablePublicId":"{{ table_public_id }}","tableDisplayName":"{{ table_display_name }}","title":"Corridoio delle Lame","gameType":"DND","publicSummary":"Demo tecnica della risoluzione autorevole delle trappole.","acceptingJoinRequests":true,"grid":{"width":4,"height":3,"difficultCells":[],"blockedCells":[],"obstacleCells":["C1","C3"],"occupiedCells":[],"walls":[],"traps":[{"trapId":"trap-c2","cell":"C2","visibility":"HIDDEN","armed":true,"lifecyclePolicy":"PERSISTENT","saveAbility":"DEXTERITY","saveDc":12,"rollMode":"NORMAL","damageExpression":"1d6","successDamage":"NONE","successMovement":"CONTINUE","failureDamage":"FULL","failureMovement":"STOP"}]}}' \
      | python3 {{ formatter }} created-session

# Mostra lo stato pubblico di un tavolo
table-status table_public_id="qr-table-04":
    @printf '\033[38;5;141m[◇ API]\033[0m Stato pubblico di %s...\n' "{{ table_public_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/public/tables/{{ table_public_id }} \
      | python3 {{ formatter }} table-status

# Risolve il QR nella partita attiva del tavolo
table-session table_public_id="qr-table-04":
    @printf '\033[38;5;141m[◇ API]\033[0m Risoluzione QR %s...\n' "{{ table_public_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/public/tables/{{ table_public_id }}/active-session \
      | python3 {{ formatter }} active-session

# Invia la richiesta di ingresso di un giocatore
request-join session_id player_reference="player-device-01" display_name="Giocatore":
    @IDEMPOTENCY_KEY="$(uuidgen | tr '[:upper:]' '[:lower:]')"; \
      printf '\033[38;5;141m[◇ API]\033[0m Richiesta di ingresso per %s...\n' "{{ player_reference }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/public/sessions/{{ session_id }}/join-requests \
        -H 'Content-Type: application/json' \
        -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
        -d '{"playerReference":"{{ player_reference }}","displayName":"{{ display_name }}"}' \
        | python3 {{ formatter }} join-request

# Elenca le richieste di ingresso in attesa del DM
pending-joins session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Richieste in attesa per %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests?status=PENDING \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-requests

# Accetta una richiesta di ingresso e crea il giocatore
accept-join session_id request_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Accettazione richiesta %s...\n' "{{ request_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests/{{ request_id }}/accept \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-resolution

# Rifiuta una richiesta di ingresso
reject-join session_id request_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Rifiuto richiesta %s...\n' "{{ request_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/join-requests/{{ request_id }}/reject \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} join-request

# Elenca i partecipanti attivi della partita
participants session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Partecipanti attivi di %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/participants \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} participants

# Crea un personaggio per il giocatore autenticato
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

# Elenca i personaggi del giocatore autenticato
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

# Elenca al DM tutti i personaggi della partita
dm-characters session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Personaggi visibili al DM nella sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/characters \
        -H "Authorization: Bearer $TOKEN" \
      | python3 {{ formatter }} characters

# Associa un personaggio a una pedina e la posiziona
create-piece session_id character_id start_cell="B2":
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Posizionamento pedina virtuale in %s...\n' "{{ start_cell }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/player/sessions/{{ session_id }}/pieces \
        -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d '{"characterId":"{{ character_id }}","representationMode":"VIRTUAL","startCell":"{{ start_cell }}"}' \
        | python3 {{ formatter }} piece

# Elenca le pedine del giocatore autenticato
my-pieces session_id:
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Pedine possedute nella sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/player/sessions/{{ session_id }}/pieces \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} pieces

# Elenca al DM tutte le pedine della partita
dm-pieces session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Pedine visibili al DM nella sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/pieces \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} pieces

# Mostra le celle dove la pedina puo arrivare
piece-reachable session_id session_piece_id:
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Celle raggiungibili dalla pedina %s...\n' "{{ session_piece_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/pieces/{{ session_piece_id }}/reachable-cells \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} piece-reachability

# Sposta la pedina e registra il movimento confermato
move-piece session_id session_piece_id destination expected_version="0" command_id="":
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then \
        printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente.\n'; \
        printf 'Esporta il token restituito da accept con: \033[1mexport BOARDHUB_PLAYER_TOKEN='\''bhp1...'\''\033[0m\n'; \
        exit 1; \
      fi; \
      COMMAND_ID="{{ command_id }}"; \
      if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Movimento pedina %s verso %s...\n' "{{ session_piece_id }}" "{{ destination }}"; \
      printf '\033[2mcommandId: %s\033[0m\n' "$COMMAND_ID"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/pieces/{{ session_piece_id }}/moves \
        -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' \
        -d "{\"destination\":\"{{ destination }}\",\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} piece-move

# Mostra la trappola in attesa di risoluzione
trap-status session_id resolution_id:
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente. Esporta BOARDHUB_PLAYER_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Stato risoluzione trappola %s...\n' "{{ resolution_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/trap-resolutions/{{ resolution_id }} \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} trap-resolution

# Esegue sul server il tiro salvezza della trappola
roll-trap session_id resolution_id expected_version command_id="":
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente. Esporta BOARDHUB_PLAYER_TOKEN.\n'; exit 1; fi; \
      COMMAND_ID="{{ command_id }}"; if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Tiro salvezza per la risoluzione %s...\n' "{{ resolution_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/trap-resolutions/{{ resolution_id }}/roll \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "{\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} trap-roll

# Prosegue il movimento residuo dopo la trappola
continue-trap session_id resolution_id expected_version command_id="":
    @set -euo pipefail; \
      TOKEN="{{ player_token }}"; \
      if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente. Esporta BOARDHUB_PLAYER_TOKEN.\n'; exit 1; fi; \
      COMMAND_ID="{{ command_id }}"; if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Prosecuzione del movimento dopo la trappola...\n'; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/trap-resolutions/{{ resolution_id }}/continue \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "{\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} piece-move

# Elenca al DM le trappole ancora da risolvere
pending-traps session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Risoluzioni di trappole in attesa per %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/trap-resolutions \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} trap-resolutions

# Il DM assume il controllo di un personaggio
assume-character session_id character_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Assunzione del controllo del personaggio %s...\n' "{{ character_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/characters/{{ character_id }}/control \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} character-control

# Il DM restituisce il controllo al giocatore
release-character session_id character_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Restituzione del controllo del personaggio %s...\n' "{{ character_id }}"; \
      curl --fail-with-body --silent --show-error -X DELETE \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/characters/{{ character_id }}/control \
        -H "Authorization: Bearer $TOKEN"; \
      printf '\033[38;5;42m● Controllo restituito al giocatore\033[0m\n'

# Sposta una pedina controllata dal DM
dm-move-piece session_id session_piece_id destination expected_version="0" command_id="":
    @set -euo pipefail; \
      TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      COMMAND_ID="{{ command_id }}"; if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Movimento DM della pedina %s verso %s...\n' "{{ session_piece_id }}" "{{ destination }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/pieces/{{ session_piece_id }}/moves \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "{\"destination\":\"{{ destination }}\",\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} piece-move

# Mostra dove puo arrivare una pedina controllata dal DM
dm-piece-reachable session_id session_piece_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Celle raggiungibili dal personaggio controllato dal DM...\n'; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/pieces/{{ session_piece_id }}/reachable-cells \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} piece-reachability

# Tiro salvezza per un personaggio controllato dal DM
dm-roll-trap session_id resolution_id expected_version command_id="":
    @set -euo pipefail; \
      TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      COMMAND_ID="{{ command_id }}"; if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Tiro salvezza eseguito dal DM per %s...\n' "{{ resolution_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/trap-resolutions/{{ resolution_id }}/roll \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "{\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} trap-roll

# Prosegue il movimento del personaggio controllato dal DM
dm-continue-trap session_id resolution_id expected_version command_id="":
    @set -euo pipefail; \
      TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      COMMAND_ID="{{ command_id }}"; if [ -z "$COMMAND_ID" ]; then COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Prosecuzione del movimento eseguita dal DM...\n'; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/trap-resolutions/{{ resolution_id }}/continue \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "{\"expectedVersion\":{{ expected_version }},\"commandId\":\"$COMMAND_ID\"}" \
        | python3 {{ formatter }} piece-move

# Mostra gli eventi filtrati del giocatore autenticato
player-events session_id:
    @TOKEN="{{ player_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token giocatore assente. Esporta BOARDHUB_PLAYER_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Eventi visibili al giocatore...\n'; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/player/sessions/{{ session_id }}/events \
        -H "Authorization: Bearer $TOKEN" | python3 {{ formatter }} events

# Mostra gli eventi completi visibili al DM
dm-events session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esporta BOARDHUB_DM_TOKEN.\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Eventi completi visibili al DM...\n'; \
      set -o pipefail; curl --fail-with-body --silent --show-error \
        {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/events \
        -H "Authorization: Bearer $TOKEN" | python3 {{ formatter }} events

# Conclude una partita e libera il suo tavolo
close-session session_id:
    @TOKEN="{{ dm_token }}"; if [ -z "$TOKEN" ]; then printf '\033[1;31m[ERRORE]\033[0m Token DM assente. Esegui: export BOARDHUB_DM_TOKEN='\''bhd1...'\''\n'; exit 1; fi; \
      printf '\033[38;5;141m[◇ API]\033[0m Chiusura sessione %s...\n' "{{ session_id }}"; \
      set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/dm/sessions/{{ session_id }}/close \
        -H "Authorization: Bearer $TOKEN" \
        | python3 {{ formatter }} closed-session

# Calcola il movimento usando la griglia salvata
move-session session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento sulla sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/sessions/{{ session_id }}/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A1","movementPoints":2}' \
      | python3 {{ formatter }} movement

# Calcola il movimento su una griglia passata nella richiesta
move-stateless:
    @printf '\033[38;5;141m[◇ API]\033[0m Calcolo movimento stateless con griglia nel body...\n'
    @set -o pipefail; curl --fail-with-body --silent --show-error -X POST {{ base_url }}/api/v1/movement/reachable-cells \
      -H 'Content-Type: application/json' \
      -d '{"characterId":"adv-01","start":"A3","movementPoints":3,"grid":{"width":6,"height":6,"difficultCells":["B3"],"blockedCells":["D3"],"obstacleCells":["C4"],"occupiedCells":[],"walls":[{"cell":"A3","direction":"EAST"}],"traps":[{"trapId":"trap-01","cell":"B4","visibility":"HIDDEN","armed":true}]}}' \
      | python3 {{ formatter }} movement

# Pubblica un evento MOVE sul broker MQTT
publish-event session_id event_id="" table_id="table-04":
    @EVENT_ID="{{ event_id }}"; \
      if [ -z "$EVENT_ID" ]; then EVENT_ID="evt-$(uuidgen | tr '[:upper:]' '[:lower:]')"; fi; \
      SEQUENCE_NUMBER="$(date +%s)"; \
      OCCURRED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"; \
      MQTT_TOPIC="boardhub/v1/venues/venue-01/tables/{{ table_id }}/events"; \
      printf '\033[38;5;208m[◈ MQTT]\033[0m Pubblicazione evento %s per sessione %s su %s...\n' "$EVENT_ID" "{{ session_id }}" "$MQTT_TOPIC"; \
      docker exec boardhub_mqtt mosquitto_pub -h localhost -t "$MQTT_TOPIC" \
        -m "{\"eventId\":\"$EVENT_ID\",\"eventType\":\"MOVE\",\"venueId\":\"venue-01\",\"tableId\":\"{{ table_id }}\",\"sessionId\":\"{{ session_id }}\",\"source\":\"SIMULATOR\",\"occurredAt\":\"$OCCURRED_AT\",\"sequenceNumber\":$SEQUENCE_NUMBER,\"payload\":{\"characterId\":\"adv-01\",\"from\":\"A1\",\"to\":\"B1\"}}"

# Legge gli eventi salvati di una partita
events session_id:
    @printf '\033[38;5;141m[◇ API]\033[0m Lettura eventi della sessione %s...\n' "{{ session_id }}"
    @set -o pipefail; curl --fail-with-body --silent --show-error {{ base_url }}/api/v1/sessions/{{ session_id }}/events \
      | python3 {{ formatter }} events

# Verifica il flusso end-to-end; richiede il backend attivo
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
