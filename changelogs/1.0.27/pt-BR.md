Atualização do **Muzix** com a **Fase 2 do Equalizador Profissional**: camada independente de **AutoEq Paramétrico**, **importação de perfis `.txt`** (para fones recentes como o *Samsung Galaxy Buds Core*) e **troca automática por dispositivo de saída**.

### Principais Melhorias (`v1.0.27`)
- **Camada AutoEq Independente & Paramétrica (`ParametricEQ`):**
  - Ao escolher um fone no **AutoEq**, o app agora baixa os **filtros paramétricos exatos** (`ParametricEQ.txt`, com fallback automático para `FixedBandEQ.txt` quando necessário).
  - O AutoEq funciona em uma **camada separada** das suas 10 bandas e controles musicais — você pode ligar/desligar ou limpar o AutoEq sem nunca apagar sua equalização manual ou seus perfis salvos.
- **Importador `.txt` (AutoEq / Squiglink / Wavelet):**
  - Nova opção **Importar .txt EQ** direto no seletor de fones do AutoEq para colar filtros de modelos mais novos que ainda não estão na lista oficial do AutoEq (como perfis *ParametricEQ*, *FixedBandEQ* ou *Wavelet GraphicEQ* gerados no Squig.link — incluindo o **Samsung Galaxy Buds Core**).
  - Perfis importados ficam salvos localmente e podem ser aplicados ou removidos a qualquer momento.
- **Troca Automática por Saída de Áudio:**
  - Nova chave **Trocar perfil por saída de áudio** dentro da seção **Meus Perfis Salvos**.
  - Vincule um perfil diferente para cada saída (**Bluetooth**, **Fio / P2**, **USB-C / DAC** ou **Alto-falante**) e o Muzix troca o equalizador automaticamente assim que você conecta ou desconecta o fone.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados, perfis salvos nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.27/LICENSE
