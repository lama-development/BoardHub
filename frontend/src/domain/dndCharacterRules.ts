export type DefenseProfile =
  | "UNARMORED"
  | "BARBARIAN_UNARMORED"
  | "MONK_UNARMORED"
  | "LEATHER"
  | "SCALE_MAIL"
  | "CHAIN_MAIL";

export type DndClassRule = {
  label: string;
  hitDie: number;
  fixedHitPointsPerLevel: number;
  defaultDefense: DefenseProfile;
  defaultShield: boolean;
};

export type DndSpeciesRule = {
  label: string;
  speedFeet: number;
  iconPath: string;
};

export const DND_SPECIES: DndSpeciesRule[] = [
  { label: "Aasimar", speedFeet: 30, iconPath: "/images/species/aasimar.png" },
  {
    label: "Dragonide",
    speedFeet: 30,
    iconPath: "/images/species/dragonide.png",
  },
  { label: "Elfo", speedFeet: 30, iconPath: "/images/species/elfo.png" },
  { label: "Gnomo", speedFeet: 30, iconPath: "/images/species/gnomo.png" },
  { label: "Goliath", speedFeet: 35, iconPath: "/images/species/goliath.png" },
  {
    label: "Halfling",
    speedFeet: 30,
    iconPath: "/images/species/halfling.png",
  },
  { label: "Nano", speedFeet: 30, iconPath: "/images/species/nano.png" },
  { label: "Orco", speedFeet: 30, iconPath: "/images/species/orco.png" },
  {
    label: "Tiefling",
    speedFeet: 30,
    iconPath: "/images/species/tiefling.png",
  },
  { label: "Umano", speedFeet: 30, iconPath: "/images/species/umano.png" },
];

export const DND_CLASSES: DndClassRule[] = [
  {
    label: "Barbaro",
    hitDie: 12,
    fixedHitPointsPerLevel: 7,
    defaultDefense: "BARBARIAN_UNARMORED",
    defaultShield: false,
  },
  {
    label: "Bardo",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "LEATHER",
    defaultShield: false,
  },
  {
    label: "Chierico",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "SCALE_MAIL",
    defaultShield: true,
  },
  {
    label: "Druido",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "LEATHER",
    defaultShield: true,
  },
  {
    label: "Guerriero",
    hitDie: 10,
    fixedHitPointsPerLevel: 6,
    defaultDefense: "CHAIN_MAIL",
    defaultShield: true,
  },
  {
    label: "Ladro",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "LEATHER",
    defaultShield: false,
  },
  {
    label: "Mago",
    hitDie: 6,
    fixedHitPointsPerLevel: 4,
    defaultDefense: "UNARMORED",
    defaultShield: false,
  },
  {
    label: "Monaco",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "MONK_UNARMORED",
    defaultShield: false,
  },
  {
    label: "Paladino",
    hitDie: 10,
    fixedHitPointsPerLevel: 6,
    defaultDefense: "CHAIN_MAIL",
    defaultShield: true,
  },
  {
    label: "Ranger",
    hitDie: 10,
    fixedHitPointsPerLevel: 6,
    defaultDefense: "SCALE_MAIL",
    defaultShield: false,
  },
  {
    label: "Stregone",
    hitDie: 6,
    fixedHitPointsPerLevel: 4,
    defaultDefense: "UNARMORED",
    defaultShield: false,
  },
  {
    label: "Warlock",
    hitDie: 8,
    fixedHitPointsPerLevel: 5,
    defaultDefense: "LEATHER",
    defaultShield: false,
  },
];

export const DEFENSE_PROFILES: Array<{ value: DefenseProfile; label: string }> =
  [
    { value: "UNARMORED", label: "Senza armatura (10 + Des)" },
    { value: "BARBARIAN_UNARMORED", label: "Difesa senza armatura Barbaro" },
    { value: "MONK_UNARMORED", label: "Difesa senza armatura Monaco" },
    { value: "LEATHER", label: "Armatura di cuoio (11 + Des)" },
    { value: "SCALE_MAIL", label: "Corazza di scaglie (14 + Des, max 2)" },
    { value: "CHAIN_MAIL", label: "Cotta di maglia (16)" },
  ];

function normalize(value: string) {
  return value.trim().toLocaleLowerCase("it-IT");
}

export function findClassRule(className: string) {
  const normalized = normalize(className);
  return (
    DND_CLASSES.find((entry) => normalize(entry.label) === normalized) ?? null
  );
}

export function findSpeciesRule(species: string) {
  const normalized = normalize(species);
  return (
    DND_SPECIES.find((entry) => normalize(entry.label) === normalized) ?? null
  );
}

export function abilityModifier(score: number) {
  return Math.floor((score - 10) / 2);
}

export function calculateHitPointMaximum(
  className: string,
  level: number,
  constitution: number,
) {
  const rule = findClassRule(className);
  if (!rule) return 10;

  const safeLevel = Math.max(1, Math.min(20, level));
  const constitutionModifier = abilityModifier(constitution);
  const firstLevel = Math.max(1, rule.hitDie + constitutionModifier);
  const laterLevel = Math.max(
    1,
    rule.fixedHitPointsPerLevel + constitutionModifier,
  );
  return firstLevel + (safeLevel - 1) * laterLevel;
}

export function calculateArmorClass(
  profile: DefenseProfile,
  dexterity: number,
  constitution: number,
  wisdom: number,
  hasShield: boolean,
) {
  const dexterityModifier = abilityModifier(dexterity);
  let baseArmorClass: number;

  switch (profile) {
    case "BARBARIAN_UNARMORED":
      baseArmorClass = 10 + dexterityModifier + abilityModifier(constitution);
      break;
    case "MONK_UNARMORED":
      baseArmorClass = 10 + dexterityModifier + abilityModifier(wisdom);
      break;
    case "LEATHER":
      baseArmorClass = 11 + dexterityModifier;
      break;
    case "SCALE_MAIL":
      baseArmorClass = 14 + Math.min(2, dexterityModifier);
      break;
    case "CHAIN_MAIL":
      baseArmorClass = 16;
      break;
    default:
      baseArmorClass = 10 + dexterityModifier;
  }

  return Math.max(0, baseArmorClass + (hasShield ? 2 : 0));
}
