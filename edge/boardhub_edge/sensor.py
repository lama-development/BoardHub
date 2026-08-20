"""Sensore simulato della plancia.

Riporta la pedina osservata su una cella. Non calcola percorsi validi e non
applica regole: l'edge riferisce soltanto cio che ha rilevato.
"""

from typing import Any, Iterator

# Percorso dimostrativo osservato sulla plancia.
DEMO_PATH = ["B2", "C2", "D2", "D3", "E3"]


def movement_observations(
    character_id: str = "adv-01",
    path: list[str] | None = None,
) -> Iterator[tuple[str, dict[str, Any]]]:
    """Genera le osservazioni di spostamento lungo un percorso fisico."""
    cells = path or DEMO_PATH
    for index in range(1, len(cells)):
        yield (
            "MOVE",
            {
                "characterId": character_id,
                "from": cells[index - 1],
                "to": cells[index],
                "observedBy": "board-sensor",
            },
        )


def dice_observation(character_id: str, faces: int, result: int) -> tuple[str, dict[str, Any]]:
    """Osservazione di un dado fisico letto dalla plancia."""
    return (
        "DICE_ROLLED",
        {
            "characterId": character_id,
            "dice": f"d{faces}",
            "result": result,
            "observedBy": "board-sensor",
        },
    )
