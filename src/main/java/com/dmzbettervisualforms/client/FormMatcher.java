package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.FormsConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;

import javax.annotation.Nullable;

/** Decide qual conjunto de roupas ("perfil") vale para uma forma. */
public final class FormMatcher {

    public static final String SSJ4_GT = "ssj4gt";
    public static final String SSJ4_DAIMA = "ssj4d";

    private FormMatcher() {
    }

    /** Perfil especial: a roupa original do DMZ sem as mangas (formas "buffed", onde o DMZ nao desenha a manga). */
    public static final String NO_ARMS = "@noarms";

    /**
     * @return {@code null} = roupa original do DMZ; senao o nome da pasta de texturas dentro de {@code textures/armor/}
     *         ({@code ssj4gt}, {@code ssj4d}, o nome escolhido no JSON das formas ou {@link #NO_ARMS}).
     *
     * <p>Prioridade: combinacao forma+stack &gt; forma (lista ou SSJ4 automatico) &gt; stack.</p>
     */
    @Nullable
    public static String profileFor(StatsData stats, FormTransition.Look look) {
        Character character = stats.getCharacter();
        if (character == null || look == null) return null;
        String race = safe(character.getRaceName());

        String formKey = look.hasForm() ? FormsConfig.formKey(race, safe(look.formGroup()), look.form()) : null;
        String stackKey = look.hasStack() ? FormsConfig.stackKey(safe(look.stackGroup()), look.stack()) : null;
        String stackGroupKey = look.hasStack() ? FormsConfig.stackKey(safe(look.stackGroup()), "*") : null;

        // 1) combinacao forma + stack
        if (formKey != null && stackKey != null) {
            FormsConfig.Entry combo = FormsConfig.lookupKey(formKey + "+" + stackKey);
            if (combo == null) combo = FormsConfig.lookupKey(formKey + "+" + stackGroupKey);
            if (combo != null) return resolve(combo, look);
        }

        // 2) forma: da lista ou SSJ4 detectado pelo modelo
        if (formKey != null) {
            FormsConfig.Entry listed = FormsConfig.lookupKey(formKey);
            String profile = listed != null ? resolve(listed, look) : bySsj4Model(look);
            if (profile != null) return profile;
        }

        // 3) stack
        if (stackKey != null) {
            FormsConfig.Entry stack = FormsConfig.lookupKey(stackKey);
            if (stack == null) stack = FormsConfig.lookupKey(stackGroupKey);
            if (stack != null) {
                String profile = resolve(stack, look);
                if (profile != null) return profile;
            }
        }

        // formas "buffed" sem perfil proprio: roupa original do DMZ (a manga fica, como antes da 2.1.0)
        return null;
    }

    @Nullable
    private static String resolve(FormsConfig.Entry entry, FormTransition.Look look) {
        if (!entry.enabled()) return null;
        return entry.profile().isEmpty() ? bySsj4Model(look) : entry.profile();
    }

    /** {@code ssj4d} / {@code ssj4gt} se a forma usa um desses modelos; senao {@code null}. */
    @Nullable
    private static String bySsj4Model(FormTransition.Look look) {
        String model = look.customModel();
        if (model.contains("ssj4d")) return SSJ4_DAIMA;
        if (model.contains("ssj4gt")) return SSJ4_GT;
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
