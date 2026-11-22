# DMZ: Damaged Clothes

Mod para o **DragonMine Z 2.2** (Forge 1.20.1). A roupa vai rasgando conforme perde durabilidade, **pixel a pixel**: cada pixel troca de uma vez de um estagio para o proximo (sem cor intermediaria).

- Hoje: **Goku gi** (`goku_gi`), 11 estagios em `assets/dmz_damaged_clothes/textures/damaged/goku_gi/00.png` a `10.png` (00 = inteira, 10 = mais destruida). Cada PNG traz as layers 1 e 2 juntas; o mod separa pelas mascaras originais do DMZ.
- Mais roupas: crie `textures/damaged/<id da roupa no DMZ>/00.png ...` (so texturas, sem codigo). O id e o nome da textura sem `_layer1.png`.
- Como funciona: mixin em `ArmorTextureResolver.resolve` (o DMZ passa o item, entao a durabilidade e conhecida). Entre dois estagios sao geradas texturas de 8 passos, na primeira vez que aparecem.
- A roupa so rasga se o item perde durabilidade. O estagio 00 e a sua textura (nao a original do DMZ).
- Compatibilidade com o DMZ-Better-Visual-Forms: a transicao de formas dele so mexe em texturas do namespace `dragonminez`; a roupa danificada pode ficar sem a transicao do SSJ4 ate ajustarmos os dois.

Instalar: suba a pasta num repositorio do GitHub, rode o workflow **Build** (aba Actions) e coloque o `.jar` em `mods`, junto com o DragonMine Z 2.2.

Ainda nao foi compilado nem testado no jogo.

## v1.1

- **Armadura de teste** (aba criativa "DMZ: Damaged Clothes"): Gi de Teste (cima, calca, botas) com **200 de durabilidade**, usando as texturas do `goku_gi` do DMZ e as 11 fases. Para ver rasgar: use `/give`/aba criativa e leve dano, ou reduza a durabilidade com `/give @s dmz_damaged_clothes:test_gi_chestplate{Damage:150}`. Conserta com barra de ferro na bigorna.
- **SSJ4**: com o modelo `ssj4gt`/`ssj4d`, a layer 1 perde camisa e mangas (372 px), igual ao mod de formas. Acompanha a barra de transformacao. Mascara: `textures/damaged/<item>/ssj4_keep.png` (opcional; sem ela nao recorta). Desliga em `ssj4Cut` no config.
- **Particulas** de 1 pixel (estilo quebrar bloco, caem no chao), na cor do pixel e saindo da regiao do corpo onde ele estava. Config: `particles`, `maxParticlesPerChange` em `config/dmz_damaged_clothes-client.toml`.
