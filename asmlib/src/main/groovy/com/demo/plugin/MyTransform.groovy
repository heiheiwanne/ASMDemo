package com.demo.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Java类描述
 *
 * @author : xmq
 * @date : 2019/1/9 上午11:21
 */
class MyTransform extends Transform {

    /**
     * 用于指明本Transform的名字，也是代表该Transform的task的名字
     */
    @Override
    String getName() {
        return "com.demo.myplugin"
    }

    /**
     * 用于指明Transform的输入类型，可以作为输入过滤的手段。注意这里我们的自定义transform是在DexTransform|MultiDexTransform 之前,
     * 所以这里我们无法处理dex类文件，因为此时dex文件还没有生成
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 用于指明Transform的作用域(整个项目作用域)
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }


    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //TransformInput：所谓Transform就是对输入的class文件转变成目标字节码文件，
        // TransformInput就是这些输入文件的抽象。目前它包括两部分：DirectoryInput集合与JarInput集合。
        transformInvocation.getInputs().each { TransformInput input ->
            //JarInput：它代表着以jar包方式参与项目编译的所有本地jar包或远程jar包，
            // 可以借助于它来动态添加jar包。
            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.name
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }
                File src = jarInput.file
                def hex = DigestUtils.md5Hex(jarInput.file.absolutePath)
                File dest = transformInvocation.getOutputProvider().getContentLocation(destName + "_" + hex, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                if (shouldProcessPreDexJar(src.absolutePath)){
                    //todothing
                    def srcJar = new JarFile(src)
                    Enumeration enumeration = srcJar.entries()
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = enumeration.nextElement()
                        //取出每一个class类，注意这里的包名是"/"分割 ，不是"."
                        String entryName = jarEntry.name
                        //todothing
//                        if (entryName.startsWith("com.demo.lib.asmdemo")) {
//                            InputStream inputStream = srcJar.getInputStream(jarEntry)
//                            //从jar中取出对应的输入流
//                            byte [] bytes = scanClass(inputStream) //jar包需要copy一个临时包最后再rename
//                            inputStream.close()
//                        }
                    }
                }

                FileUtils.copyFile(src, dest)
            }
            //DirectoryInput：它代表着以源码方式参与项目编译的所有目录结构及其目录下的源码文件，
            // 可以借助于它来修改输出文件的目录结构、已经目标字节码文件。
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //分别遍历app/build/intermediates/classes/debug下的每个目录
                String root = directoryInput.file.absolutePath
                if (!root.endsWith(File.separator)) {
                    root += File.separator
                }
                directoryInput.file.eachFileRecurse { File file ->
                    //遍历app/build/intermediates/classes/debug/*/ 下的每一个文件
                    def path = file.absolutePath.replace(root, '') //取出path看下包名是否是符合的包名信息
                    if (!(File.separator == '/')) {
                        path = path.replaceAll("\\\\", "/")
                    }
                    if (file.isFile() && shouldProcessClass(path)) {
                       FileInputStream fis =  new FileInputStream(file)
                        byte [] bytes =  scanClass(fis)
                        FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + file.name)
                        fos.write(bytes)
                        fos.close()
                        fis.close()
                    }
                }
                File dest = transformInvocation.getOutputProvider().getContentLocation(
                        directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes,
                        Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)

            }
        }
    }

    /**
     * 是否支持增量打包
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 转换过程中需要资源流的范围，在转换过程中不会被消耗，转换结束后会将资源流放回资源池去
     */
    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return super.getReferencedScopes()
    }

    /**
     * 转换输出类型，默认是 getInputTypes
     */
    @Override
    Set<QualifiedContent.ContentType> getOutputTypes() {
        return super.getOutputTypes()
    }

    /**
     * 判断扫描的类的包名是否是 ASMDemo/app/src/main/java/com/demo/lib/asmdemo/MainActivity.java
     * @param classFilePath 扫描的class文件的路径
     */
    static boolean shouldProcessClass(String classFilePath) {
        return classFilePath != null && classFilePath.startsWith("com/demo/lib/asmdemo")
    }

    /**
     * 判断jar文件【android的library可以直接排除】
     * @param jarFilepath jar文件的路径
     */
    private static boolean shouldProcessPreDexJar(String jarFilepath) {
        return !jarFilepath.contains("com.android.support") && !jarFilepath.contains("/android/m2repository")
    }


    private static byte [] scanClass(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5,cw) //ClassWriter 的代理类
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    static class ScanClassVisitor extends ClassVisitor{


        ScanClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)

            Logger.i(interfaces.toArrayString())

        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv =  cv.visitMethod(access, name, desc, signature, exceptions)
            if (name == "sendMessage") {
                mv = new ScanMethodVisitor(Opcodes.ASM5,mv)
            }
            return mv
        }
    }

    static class ScanMethodVisitor extends MethodVisitor {


        ScanMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv)
        }

        @Override
        void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.PUTFIELD && owner.equals("android/os/Message")) {
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitVarInsn(Opcodes.ALOAD, 2)
                mv.visitVarInsn(Opcodes.ALOAD, 1)
                mv.visitFieldInsn(Opcodes.PUTFIELD, "android/os/Message", "obj", "Ljava/lang/Object;")
            } else {
                mv.visitFieldInsn(opcode, owner, name, desc)
            }
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf)
        }
    }

}
