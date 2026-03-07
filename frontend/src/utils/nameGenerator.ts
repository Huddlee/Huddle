const ADJECTIVES = [
    'Swift', 'Brave', 'Calm', 'Daring', 'Eager', 'Fierce', 'Gentle', 'Happy',
    'Jolly', 'Kind', 'Lively', 'Mighty', 'Noble', 'Proud', 'Quick', 'Shy',
    'Tough', 'Vivid', 'Warm', 'Witty', 'Bold', 'Cool', 'Quiet', 'Sly',
    'Zany', 'Chill', 'Keen', 'Wild', 'Slick', 'Grand',
];

const ANIMALS = [
    'Panda', 'Tiger', 'Eagle', 'Falcon', 'Wolf', 'Bear', 'Fox', 'Hawk',
    'Lion', 'Otter', 'Raven', 'Shark', 'Whale', 'Lynx', 'Cobra', 'Crane',
    'Moose', 'Bison', 'Finch', 'Heron', 'Koala', 'Lemur', 'Owl', 'Seal',
    'Stork', 'Viper', 'Yak', 'Zebra', 'Gecko', 'Hyena',
];

/**
 * Generate a random "Adjective Animal" display name.
 */
export function generateDisplayName(): string {
    const adj = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
    const animal = ANIMALS[Math.floor(Math.random() * ANIMALS.length)];
    return `${adj} ${animal}`;
}

/**
 * Get the stored display name for the current tab session,
 * or generate and store a new one.
 */
export function getOrCreateDisplayName(): string {
    const stored = sessionStorage.getItem('huddle_display_name');
    if (stored) return stored;
    const name = generateDisplayName();
    sessionStorage.setItem('huddle_display_name', name);
    return name;
}
