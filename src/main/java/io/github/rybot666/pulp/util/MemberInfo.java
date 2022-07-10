package io.github.rybot666.pulp.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.Objects;

/**
 * Stores information about a class member (field or method)
 */
public class MemberInfo {
    public final @NotNull String owner;
    public final @NotNull String name;
    public final @NotNull Type desc;

    public MemberInfo(@NotNull String owner, @NotNull String name, @NotNull Type desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public void apply(FieldInsnNode fieldInsn) {
        fieldInsn.owner = this.owner;
        fieldInsn.name = this.name;
        fieldInsn.desc = this.desc.getDescriptor();
    }

    public static MemberInfo from(@NotNull FieldInsnNode insn) {
        Type type = Type.getType(insn.desc);
        return new MemberInfo(insn.owner, insn.name, type);
    }

    public static MemberInfo from(@NotNull String owner, @NotNull FieldNode field) {
        Type type = Type.getType(field.desc);
        return new MemberInfo(owner, field.name, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberInfo that = (MemberInfo) o;
        return owner.equals(that.owner) && name.equals(that.name) && desc.equals(that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, desc);
    }
}
