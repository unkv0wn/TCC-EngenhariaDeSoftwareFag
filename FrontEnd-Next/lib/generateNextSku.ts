const SKU_PREFIX = "REF-";
const SKU_PATTERN = /^REF-(\d+)$/i;

/**
 * Sugere o próximo código sequencial (ex: "REF-006") com base nos produtos já
 * cadastrados que seguem o padrão REF-XXX — só uma sugestão, o campo continua
 * editável pra quem quiser usar um código próprio.
 */
export function generateNextSku(products: { sku: string }[]): string {
  const maxNumber = products.reduce((max, product) => {
    const match = SKU_PATTERN.exec(product.sku);
    if (!match) return max;
    return Math.max(max, Number(match[1]));
  }, 0);

  return `${SKU_PREFIX}${String(maxNumber + 1).padStart(3, "0")}`;
}
