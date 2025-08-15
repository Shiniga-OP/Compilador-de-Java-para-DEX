package com.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.io.BufferedReader;
import java.util.Collection;
import java.io.FileReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.JarEntry;
import java.io.PrintWriter;

public class MainActivity extends Activity {
    public EditText logs;
    public ScrollView div;
    public ByteArrayOutputStream logCapturador;
    public PrintStream originalOut;
    public PrintStream originalErr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inicio);

        logs = findViewById(R.id.logs);
        div = findViewById(R.id.div);
        
        logCapturador = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(logCapturador);

        originalOut = System.out;
        originalErr = System.err;

        System.setOut(ps);
        System.setErr(ps);
    }

    public void iniciar(View v) {
		final String entradaClass = "/storage/emulated/0/es/Main.java";
		final String entradaDex = "/storage/emulated/0/es/";
		final String saidaDex = "/storage/emulated/0/es/classes.dex";
		final String androidjar = "/storage/emulated/0/android.jar";

		logCapturador.reset();
		logs.setText("");
		log("Iniciando compilação...\n");

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
				public void run() {
					try {
						String[] ecjArgs = { "-d", entradaDex, "-bootclasspath", androidjar, entradaClass };

						boolean sucesso = new org.eclipse.jdt.internal.compiler.batch.Main(
							new PrintWriter(System.out),
							new PrintWriter(System.err),
							false, null, null
						).compile(ecjArgs);

						if(sucesso){
							log("SUCESSO! Java compilado para Class\n");
							File jarArq = new File(entradaDex, "temp.jar");
							JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarArq));
							for(File a : new File(entradaDex).listFiles()){
								if(a.getName().endsWith(".class")){
									jos.putNextEntry(new JarEntry(a.getName()));
									FileInputStream fis = new FileInputStream(a);
									byte[] buf = new byte[4096]; int l;
									while((l=fis.read(buf))!=-1) jos.write(buf,0,l);
									fis.close();
									jos.closeEntry();
								}
							}
							jos.close();
						}
						log("Comando ECJ: ");
						for(String arg : ecjArgs) log(arg + " ");
						log("\n\n");
						File classArq = new File(entradaDex+"Main.class");
						if(!classArq.exists()) {
							log("ERRO: Arquivo .class não encontrado!\n");
							return;
						}
						File jarArq = new File(getFilesDir(), "temp.jar");
						JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarArq));
						jos.putNextEntry(new JarEntry("Main.class"));
						FileInputStream fis = new FileInputStream(classArq);
						byte[] buffer = new byte[4096];
						int lido;
						while((lido = fis.read(buffer)) != -1) jos.write(buffer, 0, lido);
						fis.close();
						jos.closeEntry();
						jos.close();

						log("JAR interno criado: " + jarArq.getAbsolutePath() + "\n\n");

						File dexTemp = new File(getFilesDir(), "classes.dex");
						String[] args = {"--dex", "--saida=" + dexTemp.getAbsolutePath(), jarArq.getAbsolutePath()};

						log("Comando DFoda: ");
						for(String arg : args) log(arg + " ");
						log("\n");
						try {
							com.dfoda.dexer.Main.rodar(args);
						} catch(Exception e) {
							log("Erro durante conversão DEX:\n" + e + "\n");
							return;
						}
						if(!dexTemp.exists()) {
							log("FALHA: DEX não criado\n");
							return;
						}
						File externalDex = new File(saidaDex);
						if(externalDex.exists()) externalDex.delete();
						FileInputStream fisDex = new FileInputStream(dexTemp);
						FileOutputStream fosDex = new FileOutputStream(externalDex);
						while((lido = fisDex.read(buffer)) != -1) fosDex.write(buffer, 0, lido);
						fisDex.close();
						fosDex.close();

						log("SUCESSO! DEX gerado em: " + externalDex.getAbsolutePath() + " (" + externalDex.length() + " bytes)\n");
						
						jarArq.delete();
						dexTemp.delete();
					} catch(Throwable e) {
						log("ERRO GRAVE:\n" + e + "\n");
					} finally {
						runOnUiThread(new Runnable() {
								public void run() {
									logs.append(logCapturador.toString());
									div.fullScroll(View.FOCUS_DOWN);
								}
							});
					}
				}
			});
	}

    public void log(final String msg) {
        runOnUiThread(new Runnable() {
			public void run() {
            logs.append(msg);
            div.fullScroll(View.FOCUS_DOWN);
			}
        });
    }
}
