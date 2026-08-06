function hasAllSameDigits(digits: string): boolean {
  return /^(\d)\1*$/.test(digits);
}

function calculateCheckDigit(digits: string, weights: number[]): number {
  const sum = digits
    .split("")
    .reduce((total, digit, index) => total + Number(digit) * weights[index], 0);
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

export function isValidCpf(value: string): boolean {
  const digits = value.replace(/\D/g, "");
  if (digits.length !== 11 || hasAllSameDigits(digits)) return false;

  const firstCheckDigit = calculateCheckDigit(digits.slice(0, 9), [10, 9, 8, 7, 6, 5, 4, 3, 2]);
  const secondCheckDigit = calculateCheckDigit(digits.slice(0, 10), [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);

  return digits[9] === String(firstCheckDigit) && digits[10] === String(secondCheckDigit);
}

export function isValidCnpj(value: string): boolean {
  const digits = value.replace(/\D/g, "");
  if (digits.length !== 14 || hasAllSameDigits(digits)) return false;

  const firstCheckDigit = calculateCheckDigit(digits.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const secondCheckDigit = calculateCheckDigit(digits.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);

  return digits[12] === String(firstCheckDigit) && digits[13] === String(secondCheckDigit);
}
