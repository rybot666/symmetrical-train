package io.github.rybot666.pulp.mixin_backend.transformer.instrumentation_compat;

import io.github.rybot666.pulp.PulpBootstrap;
import io.github.rybot666.pulp.mixin_backend.transformer.PulpTransformer;
import io.github.rybot666.pulp.mixin_backend.transformer.remap.FieldDefinalizationRemapper;
import io.github.rybot666.pulp.util.MemberInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Modifies classes that attempt to definalize final fields (using {@link org.spongepowered.asm.mixin.Mutable})
 *
 * All references to the definalized field are replaced with accesses to a field in the proxy class generated for this
 * class
 */
public class FieldDefinalizationHandler {
    public static boolean handle(PulpTransformer transformer, ClassNode untransformed, ClassNode transformed, ClassNode proxy) {
        boolean modifiedProxy = false;

        // Find any incompatible fields
        for (int i = 0; i < transformed.fields.size(); i++) {
            FieldNode transformedField = transformed.fields.get(i);

            for (FieldNode untransformedField : untransformed.fields) {
                if (transformedField.name.equals(untransformedField.name) && transformedField.desc.equals(untransformedField.desc)) {
                    int originalFinal = untransformedField.access & Opcodes.ACC_FINAL;
                    int transformedFinal = transformedField.access & Opcodes.ACC_FINAL;

                    if (originalFinal != transformedFinal) {
                        modifiedProxy = true;

                        // The field is incompatible with instrumentation
                        // Remove it from the transformed class
                        transformed.fields.remove(i);
                        transformed.fields.add(untransformedField);

                        // Add the transformed field to the proxy class
                        proxy.fields.add(transformedField);

                        // Generate and add marker field for initialization state
                        proxy.fields.add(new FieldNode(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                                transformedField.name.concat("$initialized"),
                                "Z",
                                null,
                                0
                        ));

                        // Register this field with the remapper
                        transformer.definalizationRemapper.register(transformed.name, transformedField.name, transformedField.desc, proxy.name);

                        // Request a retransform for all known classes using this field
                        MemberInfo mi = MemberInfo.from(transformed.name, transformedField);
                        Set<String> users = transformer.massClassState.fieldUsages.get(mi);

                        if (users != null) {
                            transformer.retransformQueue.pushAllLoaded(users);
                        }
                    }
                }
            }
        }

        return modifiedProxy;
    }
}
