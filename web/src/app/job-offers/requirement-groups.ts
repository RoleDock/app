// Preserve source order within each category and retain the original form index.
export function groupRequirements<T extends { category: string }>(requirements: readonly T[]) {
  const groups = new Map<string, { requirement: T; index: number }[]>();
  requirements.forEach((requirement, index) => {
    const entries = groups.get(requirement.category) ?? [];
    entries.push({ requirement, index });
    groups.set(requirement.category, entries);
  });
  return Array.from(groups, ([category, entries]) => ({ category, entries }));
}
