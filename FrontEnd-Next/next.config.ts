import path from "path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  turbopack: {
    root: path.join(__dirname),
  },
  // "standalone" empacota só o server + as deps realmente usadas (trace automático)
  // num diretório único — é o formato recomendado pra rodar em Docker, sem precisar
  // copiar node_modules inteiro pra imagem final. Não afeta `next dev`.
  output: "standalone",
};

export default nextConfig;
