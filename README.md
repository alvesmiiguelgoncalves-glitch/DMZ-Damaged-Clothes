# DMZ: Better Visual Forms

Addon do **DragonMine Z 2.2** (Forge 1.20.1) que junta os antigos **DMZ SSJ4 Armors** e **DMZ Form Options** e acrescenta atalhos e opções visuais.
Precisa do addon no cliente (e no servidor, se jogar em servidor, para as opções dos JSON das formas sincronizarem).

> Não foi compilado nem testado dentro do jogo. Se algo não funcionar, veja `logs/latest.log` e procure por `dmz_better_visual_forms`.

## Como compilar pelo GitHub

Suba esta pasta para um repositório novo. A aba **Actions** compila sozinha; o `.jar` fica em **Artifacts → dmz-better-visual-forms**.
Se for migrar, **remova** os jars antigos `dmz_ssj4_armors` e `dmz_form_options` da pasta `mods`.

## Atalhos (mudam em Controles → DMZ: Better Visual Forms)

| Tecla | O que faz |
|---|---|
| **F6** | Liga/desliga **Minha Aura - Estilo 3D** |
| **F7** | Liga/desliga **Contornos de Transformação** |
| **F8** | **Solta/amarra a cauda** (mesmo caminho do menu radial do DMZ). Não faz nada enquanto a forma atual força a cauda (`forceLooseTail` / `forceTiedTail`) |

F9 está livre.

## Menu Visuais do DMZ (Configurações)

- **Estado de Aura ao Transformar**: *Desativado* (usa o do próprio jogo), *Temporário* (aura por ~10 s depois de transformar; `auraStateSeconds` no toml) ou *Sempre Ativo* (Estado de Aura ligado enquanto você está transformado). É só visual, no seu cliente.
- **Raios das Transformações**: *Desativado* (sem raios de forma), *Reduzido* (raios só ~5 s depois de transformar, ao usar a aura de ki, ou com o Estado de Aura ligado; `raysSeconds` no toml) ou *Sempre Ativo* (igual ao DMZ). Os raios do surto/reserva de ki não mudam.

Também ficam em `config/dmz_better_visual_forms-client.toml`.

## Roupas por forma (antigo SSJ4 Armors)

A roupa troca quando você está em **SSJ4 do GT** (pasta `ssj4gt`) ou **SSJ4 do Daima** (pasta `ssj4d`), detectados sozinhos pelo modelo da forma.
A transição **acompanha a transformação**: na lenta (segurando G) a roupa muda junto com a carga; na instantânea, o DMZ faz uma transição curta e ela segue.
A cauda também: se a forma de destino força a cauda, ela solta/amarra quando a transformação passa da metade.

Texturas (no addon ou em qualquer resource pack, namespace `dmz_ssj4_armors`, o mesmo de antes):

```
assets/dmz_ssj4_armors/textures/armor/ssj4gt/<nome da textura>.png
assets/dmz_ssj4_armors/textures/armor/ssj4d/<nome da textura>.png
```

Só precisa incluir as peças que mudam; peça que não existe na pasta usa a roupa original do DMZ. F3 + T recarrega.

Outras formas: `config/dmz_better_visual_forms-forms.json` (o arquivo do antigo `dmz_ssj4_armors-forms.json` é copiado na primeira vez):

```json
{
  "race.dragonminez.saiyan.form.supersaiyan.supersaiyan3": "ssj3",
  "race.dragonminez.saiyan.form.supersaiyan.supersaiyanmastered": "ssjdominado"
}
```

**Stack forms (Kaio-ken etc.)**: `stack.<grupo>.<forma>` (ou `stack.<grupo>.*` para o grupo todo). Forma + stack: junte com `+`.
Prioridade: combinação > forma > stack.

```json
{
  "stack.kaioken.x10": "kaioken",
  "race.dragonminez.saiyan.form.supersaiyan.supersaiyan4+stack.kaioken.x10": "ssj4kaioken",
  "race.dragonminez.saiyan.form.supersaiyanblue.supersaiyanblue+stack.kaioken.*": "bluekaioken"
}
```

**Mangas**: nas formas "buffed" (ex.: SSJ Full Power) a roupa original do DMZ é mantida, com manga e munhequeira. Só o SSJ4 (GT e Daima) tira a manga junto com a transformação quando a pasta da forma não tem manga (`fadeSleeves` no toml).
A camisa e as mangas somem de cima para baixo (na transformação lenta e na instantânea). No SSJ4 GT e no SSJ4 Daima, cada pixel que some solta uma partícula de 1 pixel, na posição dele no corpo e com a cor dele; ela sai para fora do corpo e cai no chão (`dissolveParticles` liga/desliga e `dissolveParticleCount` limita a quantidade, no toml).

`true` = automático (pasta `ssj4d` ou `ssj4gt` pelo modelo; formas sem modelo SSJ4 ficam como estão), `false` = não troca, `"nome"` = usa a pasta `armor/nome/`. O arquivo é relido quando salva.

## Opções

| Opção | Tipo / padrão | O que faz |
|---|---|---|
| `forceTiedTail` | boolean / `false` | A cauda fica amarrada na cintura nessa forma (útil no SSJ3, em que a cauda some dentro do cabelo). |
| `forceLooseTail` | boolean / `false` | A cauda fica solta nessa forma (útil no SSJ4). Se as duas estiverem `true`, a solta vence. |
| `stackTintAffectsHairAndTail` | boolean / `true` | Vale na forma **base** (ex.: Blue). Com `false`, a tinta de uma forma empilhável (Kaio-ken) **não** pinta cabelo e cauda. |
| `stackTintHairAndTailIntensity` | número / `-1` | Vale na forma **base**. De 0 a 1: intensidade da tinta da forma empilhável **no cabelo, na cauda e nas sobrancelhas**. `-1` = usa a intensidade da própria forma empilhável. |
| `stackTintColor` | texto / `""` | Vale na forma **base**. Cor da tinta da forma empilhável **na pele** (corpo). `""` ou `"#"` = usa a cor da própria forma empilhável. |
| `stackTintIntensity` | número / `-1` | Vale na forma **base**. De 0 a 1: intensidade da tinta da forma empilhável **na pele**. `-1` = usa a intensidade da própria forma empilhável; `0` = sem tinta na pele. |
| `stackOutlinePrimaryColor` | texto / `""` | Vale na forma **base**. Cor **primária** do outline enquanto uma forma empilhável está ativa. `""` ou `"#"` = usa a cor primária do outline da própria forma empilhável. |
| `stackOutlineThicknessMultiplicator` | número / `1.0` | Vale na forma **base**. Multiplica a espessura do outline **da forma empilhável** (cada nível com a sua). `2.0` = o dobro, `1.0` = igual. |
| `stackOutlineColorMode` | texto / `""` | Vale na forma **base**. `"pulse"` ou `"gradient"` para o outline da forma empilhável. `""` = usa o modo do próprio outline da empilhável. |
| `scleraColor` | texto / `""` | Cor da esclera (parte branca do olho), por exemplo `"#FFFFFF"`. Vazio = cor padrão do jogo. |
| `noEyebrows` | boolean / `false` | Esconde as sobrancelhas: não desenha a camada de sobrancelha e usa a textura `ssj3eyebrows_eye_N` do DMZ (a do SSJ3) por cima, escolhida pelo tipo de olho do jogador. |
| `eyeliner` | boolean / `false` | Desenha o delineado nos olhos (usa a textura `ssj4_eyes_N` do DMZ, escolhida pelo tipo de olho do jogador). |
| `eyelinerColor` | texto / `"#000000"` | Cor do delineado. |
| `chargeKiAnimation` | boolean / `true` | Com `false`, o jogador **não** faz a pose de carregar ki: fica parado (idle) enquanto a aura carrega. |

## Exemplo

```json
"supersaiyan3": {
  "forceTiedTail": true,
  "noEyebrows": true
},
"supersaiyan4": {
  "forceLooseTail": true,
  "eyeliner": true,
  "eyelinerColor": "#B00000"
},
"supersaiyanblue": {
  "stackTintAffectsHairAndTail": true,
  "stackTintHairAndTailIntensity": 0.1,
  "stackTintColor": "",
  "stackTintIntensity": -1.0,
  "stackOutlinePrimaryColor": "#FF2A2A",
  "stackOutlineThicknessMultiplicator": 2.0,
  "stackOutlineColorMode": "gradient"
},
"masteredultrainstinct": {
  "chargeKiAnimation": false,
  "scleraColor": "#FFFFFF"
}
```

(O nome da forma é a chave do objeto dentro de `"forms"`; coloque as opções junto das outras, por exemplo perto de `"hideTail"`.)

Depois de salvar, use `/dmzreload config`.

## Observações

- **Ordem no arquivo:** se o DMZ regravar o arquivo (atualização de versão), as opções novas vão para o fim de cada forma, não ao lado do `hideTail`.
- **Olhos:** esclera, sobrancelha e delineado valem para o rosto de estilo humano/saiyajin. Raças com rosto próprio (Namekuseijin, Freeza etc.) não usam essas opções.
- **Delineado:** só aparece se existir `ssj4_eyes_N.png` para o tipo de olho do jogador (o DMZ já traz as do SSJ4).
- **Tinta do Kaio-ken:** a opção vai na forma **base**. Afeta o cabelo, a cauda desenhada na camada do corpo e as partes de raça (cauda, antenas). As sobrancelhas (cor do cabelo no rosto) também usam a tinta do cabelo/cauda. Os olhos continuam com a tinta normal.
- **Mixins:** o addon altera classes do DMZ 2.2. Se uma opção não fizer nada, veja o `logs/latest.log` e procure por `dmz_form_options` (o jogo abre mesmo se algum mixin falhar).
- Não foi testado dentro do jogo.

## Compatibilidade com o DMZ Face Animations (dmz-blink)

O dmz-blink troca o caminho da textura dentro de `renderColoredLayer`. Este addon desenha os olhos pelo mesmo método
(esclera, sobrancelha, delineado), então a piscada e os olhos angulares do dmz-blink devem continuar valendo com as
opções daqui. Com `noEyebrows` o addon nem desenha a camada de sobrancelha (que o dmz-blink troca por versões angulares/piscando) e ainda aplica o apagador do SSJ3 por cima. Não foi testado junto.

## Se o cabelo não mudar com a tinta

Ao carregar o jogo, procure no `logs/latest.log` a linha `hair/tail tint override is active`. Se ela aparece, o addon
está trocando a tinta do cabelo; se o cabelo continua igual, o estilo de cabelo provavelmente tem cores próprias por
mecha, que o DMZ não tinge. Se a linha não aparece, a opção não foi lida (confira a forma base e `/dmzreload config`).

## v1.1.2

- O cabelo agora recebe a tinta também quando o estilo tem **cor própria por mecha** (o DMZ ignora a tinta global nesse caso). Mechas que herdam a cor global não são tintadas duas vezes.

## v1.1.3

- Corrigido: os mixins do cabelo e das partes da raça não eram aplicados (o método era `static` e o do DMZ não é). Por isso a tinta nunca chegava no cabelo.

## v1.2.0 — opções da forma base para formas empilháveis (Kaio-ken)

As opções novas ficam na forma **base** (ex.: Blue) e dizem como uma forma empilhável (Kaio-ken) fica **em cima dela**. Nada muda se você não escrever as opções, e sem forma base ativa (Kaio-ken sozinho) vale a configuração da própria forma empilhável.

- **Pele:** `stackTintColor` e `stackTintIntensity`. Com `""` e `-1`, a pele usa a tinta da própria forma empilhável.
- **Cabelo, cauda e sobrancelhas:** continuam em `stackTintAffectsHairAndTail` e `stackTintHairAndTailIntensity`, sempre com a **cor** da forma empilhável.
- **Outline:** `stackOutlinePrimaryColor`, `stackOutlineThicknessMultiplicator` e `stackOutlineColorMode`. Quando uma delas está preenchida (cor diferente de vazio, multiplicador diferente de `1.0` ou modo diferente de vazio), o outline é o da própria forma empilhável, com estas trocas:
  - cor **primária** = `stackOutlinePrimaryColor` (vazio ou `"#"` = primária da forma empilhável);
  - espessura = espessura da forma empilhável × `stackOutlineThicknessMultiplicator` (cada nível do Kaio-ken multiplica o seu valor);
  - modo de cor = `stackOutlineColorMode` (vazio = o da forma empilhável).

  Cor secundária, fogo e brilho vêm sempre da forma empilhável, que precisa ter `outlineShader.enabled: true`. Com tudo no padrão, vale o comportamento original do DMZ.
- **Tinta x2 → x20:** cada nível do Kaio-ken é uma forma separada com seu próprio `tintIntensity`. Deixe `stackTintIntensity` e `stackTintHairAndTailIntensity` em `-1` para cada nível usar o seu. Um valor fixo vale para todos os níveis.
- Removidas as opções da 1.2.0 anterior (`tintColor` vazio, `primaryColor` vazio e `addToBaseOutline`).

Exemplo (na forma base):
```json
"stackTintColor": "",
"stackTintIntensity": -1.0,
"stackTintHairAndTailIntensity": -1.0,
"stackOutlinePrimaryColor": "#FF2A2A",
"stackOutlineThicknessMultiplicator": 2.0,
"stackOutlineColorMode": "gradient"
```
