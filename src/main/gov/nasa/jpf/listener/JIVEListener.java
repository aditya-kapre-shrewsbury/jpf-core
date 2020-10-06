package gov.nasa.jpf.listener;

import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.vm.ArrayFields;
import gov.nasa.jpf.vm.Backtracker.RestorableState;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.ISTORE;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Fields;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.RestorableVMState;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VMListener;
import gov.nasa.jpf.report.Statistics;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;


public class JIVEListener implements VMListener, SearchListener {

	public File file = null;
	public FileWriter fstream = null;
	public BufferedWriter out = null;
	public int counter = 1;
	public static int id = 1;
	public static int thisid = -2;
	static boolean blObjCreated = false;
	static boolean blChoiceGeneratorAdvanced = false;
	static boolean blThreadChoice = false;
	static boolean isPathStarted = false;
	static Hashtable<String, Integer> type_index = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> oid_index = new Hashtable<String, Integer>();

	private enum EventKind {
		PATH_START, PATH_END, NULL;
	}

	EventKind eventKind = null;

	static List lsIrrelevantTypes = new ArrayList();
	static List<String> lsIrrelevantMethods = new ArrayList<String>();

	/** set of final Strings required **/
	final String INIT_METHOD = "<init>";
	final String CLASSINIT_METHOD = "<clinit>";
	final String RUN_METHOD = "run";
	final String INNERCLASS_SEPERATOTR = "$";
	static Stack treePath = new Stack();
	static Stack methodCall = new Stack();
	/** counters for the listener methods **/
	static StackFrame sf = null;
	static Map<String, Object> mpOld = new HashMap<String, Object>();
	static int mpSize = 0;

	public JIVEListener() {
		try {
			file = new File("jive.txt");
			file.createNewFile();
			fstream = new FileWriter(file);
			if (file != null) {
			}
			initializeIrrelevantTypesList();
			initializeIrrelevantMethodsList();
			startSystem();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stateAdvanced(Search search) {
	}

	@Override
	public void stateProcessed(Search search) {
		// System.out.println("LTNR: stateProcessed :id="+search.getStateId());
	}

	@Override
	public void stateBacktracked(Search search) {
		// TODO Auto-generated method stub
		// System.out.println("LTNR: stateBacktracked : id="+search.getStateId());
		/** Create events : PATH_END **/
		try {
			fstream.write("event\n");
			Object val = treePath.pop();
			fstream.write("parentid=" + val + "\n");
			++id;
			--thisid;
			fstream.write("id=" + id + "\n");
			fstream.write("thisid=" + thisid + "\n");
			fstream.write("thread="
					+ search.getVM().getLastTransition().getThreadInfo().getId() + "\n");
			fstream.write("kind=Path End" + "\n");
			fstream.write("file=" + search.getVM().getPath().getApplication() + ".java"
					+ "\n");
			fstream.write("line=" + search.getVM().getCurrentThread().getLine()
					+ "\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			/** for details **/
			fstream.write("totalNumOfChoices="
					+ search.getVM().getChoiceGenerator()
							.getTotalNumberOfChoices() + "\n");
			fstream.write("sourceLocation="
					+ search.getVM().getChoiceGenerator().getSourceLocation()
					+ "\n");
			fstream.write("choiceType="
					+ search.getVM().getChoiceGenerator().getSourceLocation()
					+ "\n");
			fstream.write("nextChoice="
					+ search.getVM().getChoiceGenerator().getNextChoice()
					+ "\n");
			fstream.write("endevent\n");
			fstream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void statePurged(Search search) {
		// System.out.println("LTNR: statePurged :id="+search.getStateId());
	}

	@Override
	public void stateStored(Search search) {
		// System.out.println("LTNR: stateStored id="+search.getStateId());
	}

	@Override
	public void stateRestored(Search search) {
		// System.out.println("LTNR: stateRestored id="+search.getStateId());
	}

	@Override
	public void propertyViolated(Search search) {
		// System.out.println("LTNR: propertyViolated ="+search.getLastError().getDetails());
		String strError = search.getLastError().getDetails()
				.replaceAll("\n", "").replaceAll("\t", " ");
		String strEx = "Error:"
				+ search.getLastError()
						.getDetails()
						.substring(
								0,
								search.getLastError().getDetails()
										.indexOf("\n"));
		try {
			fstream.write("event\n");
			fstream.write("parentid=" + treePath.peek() + "\n");
			++id;
			--thisid;
			fstream.write("id=" + id + "\n");
			fstream.write("thisid=" + thisid + "\n");
			fstream.write("thread=" + search.getVM().getCurrentThread().getId()
					+ "\n");
			fstream.write("kind=Path Start" + "\n");
			fstream.write("file=" + search.getVM().getPath().getApplication() + ".java"
					+ "\n");
			fstream.write("line=" + search.getVM().getCurrentThread().getLine()
					+ "\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			/** for details **/
			fstream.write("totalNumOfChoices="
					+ search.getVM().getChoiceGenerator()
							.getTotalNumberOfChoices() + "\n");
			fstream.write("sourceLocation="
					+ search.getVM().getChoiceGenerator().getSourceLocation()
					+ "\n");
			fstream.write("choiceType="
					+ search.getVM().getChoiceGenerator().getSourceLocation()
					+ "\n");
			fstream.write("instructionCG=Property Violated\n");
			fstream.write("nextChoice="
					+ search.getVM().getChoiceGenerator().getNextChoice()
					+ "\n");
			fstream.write("varToBeChanged=" + strEx.toUpperCase() + "\n");
			fstream.write("endevent\n");
			fstream.flush();

			// for object diag
			fstream.write("event\n");
			fstream.write("parentid=" + treePath.peek() + "\n");
			++id;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=" + search.getVM().getCurrentThread().getId()
					+ "\n");
			fstream.write("thisid=" + thisid + "\n");
			fstream.write("kind=Exception Throw" + "\n");
			if (search.getVM().getChoiceGenerator().getSourceLocation() == null) {
				fstream.write("file=unavailable" + "\n");
			} else {
				fstream.write("file="
						+ search.getVM().getChoiceGenerator()
								.getSourceLocation() + "\n");
			}
			fstream.write("line=" + search.getVM().getCurrentThread().getLine()
					+ "\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("exception=" + strEx.toUpperCase() + "\n");
			fstream.write("thrower=" + methodCall.peek() + "\n");
			fstream.write("framePopped=true\n");
			fstream.write("endevent\n");
			fstream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void searchStarted(Search search) {
		// System.out.println("LTNR: searchStarted");

	}

	@Override
	public void searchConstraintHit(Search search) {
	}

	@Override
	public void searchFinished(Search search) {
	}

	boolean isRelevantType(String type) {
		if (lsIrrelevantTypes.contains(type)) {
			return false;
		}
		/** when there is array, we need to do filed write **/
		else if (type.startsWith("[")) {
			return true;
		} else {
			return true;
		}

	}

	boolean isRelevantMethod(String method) {
		for (String s : lsIrrelevantMethods) {
			if (method.contains(s)) {
				return false;
			}
		}
		return true;
	}

	void initializeIrrelevantTypesList() {
		lsIrrelevantTypes.add("Ljava/lang/Thread;");
		lsIrrelevantTypes.add("Ljava/lang/ThreadGroup;");
		lsIrrelevantTypes.add("Ljava/lang/String;");
		lsIrrelevantTypes.add("[C");
		lsIrrelevantTypes.add("Ljava/lang/Class;");
		lsIrrelevantTypes.add("Ljava/lang/Thread$Permit;");
		lsIrrelevantTypes.add("Ljava/lang/ClassLoader;");
		lsIrrelevantTypes.add("[Ljava.lang.String;");
		lsIrrelevantTypes.add("Ljava/lang/Boolean;");
		lsIrrelevantTypes.add("[I");
		lsIrrelevantTypes.add("[Ljava.io.ObjectStreamField;");
		lsIrrelevantTypes.add("Ljava/lang/String$CaseInsensitiveComparator;");
		lsIrrelevantTypes.add("Ljava/lang/Thread$State;");
		lsIrrelevantTypes.add("[Ljava.lang.Thread$State;");
		lsIrrelevantTypes.add("Lgov/nasa/jpf/ConsoleOutputStream;");
		lsIrrelevantTypes.add("Lsun/misc/Unsafe;");
		lsIrrelevantTypes.add("Ljava/lang/reflect/Field;");
		lsIrrelevantTypes.add("Ljava/util/Properties;");
		lsIrrelevantTypes.add("Ljava/lang/Runtime;");
		lsIrrelevantTypes.add("[Ljava.util.Hashtable$Entry;");
		lsIrrelevantTypes.add("Ljava/util/Hashtable$Entry;");
		lsIrrelevantTypes.add("Ljava/lang/System$1;");
		lsIrrelevantTypes.add("Ljava/lang/StringBuilder;");
		lsIrrelevantTypes.add("[Ljava.lang.Thread;");
		lsIrrelevantTypes.add("boolean");
		lsIrrelevantTypes.add("char");
		lsIrrelevantTypes.add("short");
		lsIrrelevantTypes.add("int");
		lsIrrelevantTypes.add("long");
		lsIrrelevantTypes.add("float");
		lsIrrelevantTypes.add("double");
		lsIrrelevantTypes.add("byte");
		lsIrrelevantTypes.add("[F");
		lsIrrelevantTypes.add("[D");
		lsIrrelevantTypes.add("[Z");
		lsIrrelevantTypes.add("[B");
		lsIrrelevantTypes.add("[S");
		lsIrrelevantTypes.add("[J");
		lsIrrelevantTypes.add("void");
	}

	void initializeIrrelevantMethodsList() {
		lsIrrelevantMethods.add("<init>");
		lsIrrelevantMethods.add("<clinit>");
		lsIrrelevantMethods.add("Object.registerNatives");
		lsIrrelevantMethods.add("ClassLoader.getSystemClassLoader");
		lsIrrelevantMethods.add("Class.getPrimitiveClass");
		lsIrrelevantMethods.add("sun.misc.");
		lsIrrelevantMethods.add("java.lang.");
		lsIrrelevantMethods.add("java.util.");
	}

	void startSystem() {

		try {
			fstream.write("event\n");
			id++;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=SYSTEM\n");
			fstream.write("kind=System Start\n");
			fstream.write("file=unavailable\n");
			fstream.write("line=-1\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("endevent\n");

			fstream.write("event\n");
			id++;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=0\n");
			fstream.write("kind=Thread Create\n");
			fstream.write("file=unavailable\n");
			fstream.write("line=-1\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("newthread=0\n");
			fstream.write("th_name=main\n");
			fstream.write("endevent\n");

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}
	
	/**
	 * Aditya :: New interfaces
	 */

	@Override
	public void searchProbed(Search search) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void vmInitialized(VM vm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		// System.out.println("LTNR: executeInstruction");
	}

	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
			Instruction executedInstruction) {
		
		//Instruction insn = vm.getLastInstruction();
		//newjivejpf
		Instruction insn = executedInstruction;
		
		// System.out.println("LNTR::instructionExecuted::"+insn.getMnemonic());
		if (insn.getMnemonic().equalsIgnoreCase("nativereturn")
				|| insn.getMnemonic().contains("istore")) {
			sf = vm.getCurrentThread().getTopFrame();
			if (sf != null) {
				if (sf.getLocalVars() != null) {
					LocalVarInfo[] lvi = sf.getLocalVars();
					for (LocalVarInfo l : lvi) {
						if (l.getName().equalsIgnoreCase("args")
								|| l.getName().startsWith("[L")) {
							if (sf.getLocalValueObject(l).toString() != null
									&& sf.getLocalValueObject(l).toString()
											.contains("java")) {
								continue;
							}
						}
					}
				}
			}

			LocalVarInfo[] lvi = sf.getLocalVars();
			String varToBeChanged = "IV";
			if (sf.getLocalVars() != null)
				if (insn.getMnemonic().startsWith("istore")
						&& eventKind == EventKind.PATH_START) {
					if (lvi != null)
						for (LocalVarInfo l : lvi) {
							if (l.getName().equalsIgnoreCase("args")
									|| l.getName().startsWith("[L")
									|| sf.getLocalValueObject(l).toString()
											.contains("java")) {
								continue;
							}
							String oldval, newval = null;
							if (mpOld.get(l.getName()) != null) {
								oldval = mpOld.get(l.getName()).toString();
								newval = sf.getLocalValueObject(l).toString();
								if (!oldval.equalsIgnoreCase(newval)) {
									varToBeChanged = l.getName();
								}
							}
						}
					try {
						if (blThreadChoice) {
							blThreadChoice = false;
						} else {
							fstream.write("varToBeChanged=" + varToBeChanged
									+ "\n");
							fstream.write("endevent\n");
							fstream.flush();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					eventKind = EventKind.NULL;
				}

			mpOld.clear();
			if (lvi != null)
				for (LocalVarInfo l : lvi) {
					if (l.getName().equalsIgnoreCase("args")
							|| l.getName().startsWith("[L")) {
						if (sf.getLocalValueObject(l).toString() != null
								&& sf.getLocalValueObject(l).toString()
										.contains("java")) {
							continue;
						}
					}
					mpOld.put(l.getName(), sf.getLocalValueObject(l));
				}
		}
	}

	@Override
	public void threadStarted(VM vm, ThreadInfo startedThread) {
		try {

			//ThreadInfo ti = vm.getLastThreadInfo();
			//newjivejpf
			ThreadInfo ti = startedThread;
			fstream.write("event\n");
			id++;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=" + ti.getId() + "\n");
			fstream.write("kind=Thread Create\n");
			fstream.write("file=unavailable\n");
			fstream.write("line=-1\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("newthread=" + ti.getId() + "\n");
			fstream.write("th_name=" + ti.getName() + "\n");
			System.out.println("THREAD_CREATE = " + ti.getId());
			fstream.write("endevent\n");

			/** Relevance :: Method call to constructor of thread **/
			String target = ti.getStateDescription().substring(
					ti.getStateDescription().indexOf("thread ") + 7,
					ti.getStateDescription().indexOf(":"));
			if (target.equals("java.lang.Thread")) {
				return;
			}

			// Method Call
			fstream.write("event\n");
			id++;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=" + ti.getId() + "\n");
			fstream.write("**Thread started Method Call\n");
			fstream.write("kind=Method Call\n");
			fstream.write("file=unavailable\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("line=-1\n");
			fstream.write("caller=" + methodCall.peek() + "\n");
			String constructor = null;
			if (target.contains("$"))
				constructor = target.substring(target.indexOf("$"),
						target.length());

			fstream.write("target=" + target + "#run" + "\n");
			String strSignature = target;
			if (ti.getId() > 0) {
				strSignature = strSignature + ":"
						+ ti.getId() + ";/" + "run()V";
				;
			} else {
				strSignature = strSignature + ";/" + "run()V";
				;
			}
			fstream.write("signature=" + strSignature + "\n");
			fstream.write("endevent\n");
			// print
			System.out.println("THREAD_STARTED :: caller = "
					+ methodCall.peek());
			System.out.println("THREAD_STARTED :: target = " + target + "#run");

			// METHOD_ENTERED
			fstream.write("event\n");
			id++;
			fstream.write("id=" + id + "\n");
			fstream.write("thread=" + ti.getId() + "\n");
			fstream.write("kind=Method Entered\n");
			fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
			fstream.write("line=-1\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			fstream.write("endevent\n");
			// END METHOD_ENTERED

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void threadBlocked(VM vm, ThreadInfo blockedThread, ElementInfo lock) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void threadWaiting(VM vm, ThreadInfo waitingThread) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void threadNotified(VM vm, ThreadInfo notifiedThread) {
		// TODO Auto-generated method stub
	}

	@Override
	public void threadInterrupted(VM vm, ThreadInfo interruptedThread) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void threadTerminated(VM vm, ThreadInfo terminatedThread) {
		ThreadInfo ti = vm.getCurrentThread();
		//ThreadInfo tii = vm.getLastThreadInfo();
		//newjivejpf
		ThreadInfo tii = terminatedThread;
		System.out.println();
		System.out.println("THREAD_EXIT = Current = " + ti.getName());
		System.out.println("THREAD_EXIT = Last    = " + tii.getName());
		
	}

	@Override
	public void threadScheduled(VM vm, ThreadInfo scheduledThread) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void loadClass(VM vm, ClassFile cf) {
		// TODO Auto-generated method stub
	}

	@Override
	public void classLoaded(VM vm, ClassInfo loadedClass) {
		//ClassInfo ci = vm.getLastClassInfo();
		//newjivejpf
		ClassInfo ci = loadedClass;
		
		if (isRelevantType(ci.getName())) {
			if (!ci.getName().contains(".")) {
				System.out.println("class loaded =" + ci.getName());
				Iterator<MethodInfo> itr = ci.declaredMethodIterator();
				while (itr.hasNext()) {
					MethodInfo mi = itr.next();
					System.out.println(mi);
					LocalVarInfo[] lvi = mi.getLocalVars();
					if (lvi != null)
						for (LocalVarInfo l : lvi) {
							System.out.println(l.getName() + "="
									+ l.getSignature());
						}
				}
			}
		}

	}

	@Override
	public void objectCreated(VM vm, ThreadInfo currentThread, ElementInfo newObject) {	

		String constructor = null;
		String constructor_called = null;
		//ElementInfo ei = vm.getLastElementInfo();
		//newjivejpf
		ElementInfo ei = newObject;
		
		int iObjRef = ei.getObjectRef();
		ElementInfo objInfo = vm.getElementInfo(iObjRef);
		String type = objInfo.getType();
		//ThreadInfo thInfo = vm.getLastThreadInfo();
		//newjivejpf
		ThreadInfo thInfo = currentThread;

		try {
			if (isRelevantType(type) && isPathStarted) {

				System.out.println();
				System.out.println("OBJECT_NEW = " + objInfo.getType());

				// Info:: Creating [New Object]+[Field Write] for Array
				if (objInfo.getType().startsWith("[L")) {

					int size = objInfo.arrayLength();
					String context = objInfo.getType().substring(
							objInfo.getType().indexOf("[L") + 2,
							objInfo.getType().length() - 1);
					/** New Object for Array **/
					fstream.write("event\n");
					id++;
					fstream.write("id=" + id + "\n");
					fstream.write("thread=" + vm.getCurrentThread().getId()
							+ "\n");
					fstream.write("kind=New Object\n");
					fstream.write("file=unavailable\n");
					fstream.write("timestamp=" + System.currentTimeMillis()
							+ "\n");
					fstream.write("line=-1\n");
					fstream.write("object=" + objInfo.hashCode() + "\n");
					fstream.write("type=" + context + "[];" + "\n");
					fstream.write("elements=" + size + "\n");
					fstream.write("endevent\n");

					/** Field Write for Array **/
					for (int i = 0; i < size; i++) {
						fstream.write("event\n");
						id++;
						fstream.write("id=" + id + "\n");
						fstream.write("thread="
								+ thInfo.getId() + "\n");
						fstream.write("**Field Write For Array\n");
						fstream.write("kind=Field Write\n");
						fstream.write("file=unavailable\n");
						fstream.write("timestamp=" + System.currentTimeMillis()
								+ "\n");
						fstream.write("line=-1\n");
						fstream.write("target=" + objInfo.hashCode() + "\n");
						fstream.write("value=" + context + ":" + (i + 1) + "\n");
						fstream.write("type=" + context + "[];" + "\n");
						fstream.write("field=" + i + "\n");
						fstream.write("endevent\n");
					}
					return;
				}

				blObjCreated = true;
				constructor = type.substring(1, type.length() - 1);
				// System.out.println("constructor = "+constructor);
				if (constructor.contains("$")) {
					constructor_called = constructor
							+ "#"
							+ constructor.substring(constructor.indexOf("$"),
									constructor.length());
				} else if (constructor.contains("/")) {
					constructor_called = constructor.substring(
							constructor.lastIndexOf("/") + 1,
							constructor.length());
					constructor_called = constructor_called + "#"
							+ constructor_called;
					System.out.println("constructor_called= "
							+ constructor_called);
				} else {
					constructor_called = constructor + "#" + constructor;
				}
				System.out.println("constructor_called= " + constructor);

				// Info:: For Normal Objects Counting number of instance
				// variables**/
				int iNoOfInstanceVars = 0;
				int iTotalFields = objInfo.getNumberOfFields();
				for (int k = 0; k < iTotalFields; k++) {
					FieldInfo f = objInfo.getFieldInfo(k);
					if (!f.getFullName().contains("java.lang.")) {
						iNoOfInstanceVars++;
					}
				}

				int index = 1;
				if (type_index.get(type) == null) {
					type_index.put(type, index);
					oid_index.put(objInfo.toString(), index);
				} else {
					index = type_index.get(type);
					index++;
					type_index.remove(type);
					type_index.put(type, index);
					oid_index.put(objInfo.toString(), index);
					System.out.println("oid_index = " + iObjRef + " :: "
							+ index);
					System.out.print(objInfo + "  ");
				}

				// Info:: New Object for normal non-array objects
				fstream.write("event\n");
				if (treePath.isEmpty()) {
					fstream.write("parentid=-1" + "\n");
				} else {
					fstream.write("parentid=" + treePath.peek() + "\n");
				}
				id++;
				fstream.write("id=" + id + "\n");
				if (thInfo != null) {
					fstream.write("thread=" + thInfo.getId() + "\n");
				} else {
					fstream.write("thread=\" \"\n");
				}
				fstream.write("kind=New Object\n");
				String removedLtype = objInfo.getType().substring(1,
						objInfo.getType().length());
				fstream.write("type=" + removedLtype + "\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("object=" + iObjRef + "\n");
				fstream.write("file=" + vm.getPath().getApplication()
						+ ".java" + "\n");
				/** set line number as the last line of last method **/
				fstream.write("line=1\n");
				blObjCreated = false;
				/** to signify event end **/
				fstream.write("endevent\n");

				// Info:: Constructor Call for Normal Objects
				// Method Call
				fstream.write("event\n");
				id++;
				fstream.write("id=" + id + "\n");
				fstream.write("thread=" + thInfo.getId() + "\n");
				fstream.write("**constructor call\n");
				fstream.write("kind=Method Call\n");
				fstream.write("file=unavailable\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("line=-1\n");
				fstream.write("caller=" + methodCall.peek() + "\n");
				fstream.write("target=" + constructor_called + "\n");

				String strSignature = constructor + ":"
						+ index
						// +iObjRef
						// + (vm.getLastThreadInfo().getId()+1)
						+ ";/"
						+ constructor.substring(constructor.indexOf("$") + 1,
								constructor.length()) + "()V";

				fstream.write("signature=" + strSignature + "\n");
				fstream.write("endevent\n");
				// print
				System.out.println("OBJECT_CREATED :: caller = "
						+ methodCall.peek());
				System.out.println("OBJECT_CREATED :: target = "
						+ constructor_called);

				// METHOD_ENTERED
				fstream.write("event\n");
				id++;
				fstream.write("id=" + id + "\n");
				fstream.write("thread=" + thInfo.getId() + "\n");
				fstream.write("kind=Method Entered\n");
				fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
				fstream.write("line=-1\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("endevent\n");
				// END METHOD_ENTERED

				// Method exit
				fstream.write("event\n");
				id++;
				fstream.write("id=" + id + "\n");
				fstream.write("thread=" + thInfo.getId() + "\n");
				fstream.write("kind=Method Exit\n");
				fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
				fstream.write("line=-1\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("returner=" + constructor + "\n");
				fstream.write("endevent\n");
				// END METHOD_EXIT

				/** Field Write for instance variables **/
				for (int i = 0, field_num = 0; i < iTotalFields; i++) {/*
					FieldInfo f = objInfo.getFieldInfo(i);
					if (!f.getFullName().contains("java.lang.")) {
						// System.out.println(f.getName());
						Fields ff = objInfo.getFields();
						// System.out.println(f.getValueObject(ff));
						fstream.write("event\n");
						id++;
						fstream.write("id=" + id + "\n");
						fstream.write("thread="
								+ vm.getLastThreadInfo().getId() + "\n");
						fstream.write("**Instance Variable Field Write\n");
						fstream.write("kind=Field Write\n");
						fstream.write("file=unavailable\n");
						fstream.write("timestamp=" + System.currentTimeMillis()
								+ "\n");
						fstream.write("line=-1\n");
						fstream.write("target=" + iObjRef + "\n");

						String str = null;
						if (f.getValueObject(ff) == null) {
							str = "null";
							fstream.write("value=" + str + "\n");
						} else {
							str = f.getValueObject(ff).toString();
							System.out.println("str=" + str);
							if (str.contains("@")) {
								str = str.substring(0, str.indexOf("@"));
								System.out.println("str = " + str);
							}
							fstream.write("value="
									+ str
									+ ":"
									+ oid_index.get(f.getValueObject(ff)
											.toString()) + "\n");
						}
						// fstream.write("value="+f.getValueObject(ff)+"\n");
						fstream.write("type=" + objInfo.getType() + "[];"
								+ "\n");
						fstream.write("field=" + field_num + "\n");
						field_num++;
						fstream.write("endevent\n");
					}
				*/}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void objectReleased(VM vm, ThreadInfo currentThread, ElementInfo releasedObject) {
		//ElementInfo ei = vm.getLastElementInfo();
		//newjivejpf
		ElementInfo ei = releasedObject;
		
		int iObjRef = ei.getObjectRef();
		ElementInfo objInfo = vm.getElementInfo(iObjRef);
		for (int i = 0, field_num = 0; i < objInfo.getNumberOfFields(); i++) {
			FieldInfo f = objInfo.getFieldInfo(i);
			if (!f.getFullName().contains("java.lang.")) {

				System.out.print("objectReleased : name : " + f.getName());
				Fields ff = objInfo.getFields();
				// System.out.println("="+f.getValueObject(ff));
				// System.out.println("-- " +iObjRef);
				try {
					fstream.write("event\n");
					id++;
					fstream.write("id=" + id + "\n");
					fstream.write("thread=" + currentThread.getId()
							+ "\n");
					fstream.write("kind=Field Write\n");
					fstream.write("file=unavailable\n");
					fstream.write("timestamp=" + System.currentTimeMillis()
							+ "\n");
					fstream.write("line=-1\n");
					fstream.write("target=" + iObjRef + "\n");
					String str = null;
					if (f.getValueObject(ff) == null) {
						str = "null";
						fstream.write("value=" + str + "\n");
					} else {
						str = f.getValueObject(ff).toString();
						System.out
								.println("oid_index.get(f.getValueObject(ff))="
										+ oid_index.get(f.getValueObject(ff)));
						if (str.contains("@")) {
							str = str.substring(0, str.indexOf("@"));
							System.out.println("str = " + str);
						}
						fstream.write("value="
								+ str
								+ ":"
								+ oid_index
										.get(f.getValueObject(ff).toString())
								+ "\n");
					}
					String typeWithNoL = objInfo.getType().substring(1,
							objInfo.getType().length());
					fstream.write("type=" + typeWithNoL + "\n");
					fstream.write("field=" + f.getName() + "\n");
					field_num++;
					fstream.write("endevent\n");
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		}	
	}

	@Override
	public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
		// TODO Auto-generated method stub
	}

	@Override
	public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void objectWait(VM vm, ThreadInfo currentThread, ElementInfo waitingObject) {
		// System.out.println("LTNR: objectWait");
	}

	@Override
	public void objectNotify(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {
		// System.out.println("LTNR: objectNotify");
	}

	@Override
	public void objectNotifyAll(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {
		// System.out.println("LTNR: objectNotifyAll");	
	}

	@Override
	public void objectExposed(VM vm, ThreadInfo currentThread, ElementInfo fieldOwnerObject,
			ElementInfo exposedObject) {
		// TODO Auto-generated method stub
	}

	@Override
	public void objectShared(VM vm, ThreadInfo currentThread, ElementInfo sharedObject) {
		// TODO Auto-generated method stub
	}

	@Override
	public void gcBegin(VM vm) {
		
	}

	@Override
	public void gcEnd(VM vm) {
		
	}

	@Override
	public void exceptionThrown(VM vm, ThreadInfo currentThread, ElementInfo thrownException) {
		// System.out.println("LTNR: exceptionThrown");		
	}

	@Override
	public void exceptionBailout(VM vm, ThreadInfo currentThread) {
		
	}

	@Override
	public void exceptionHandled(VM vm, ThreadInfo currentThread) {
		// TODO Auto-generated method stub
	}

	@Override
	public void choiceGeneratorRegistered(VM vm, ChoiceGenerator<?> nextCG, ThreadInfo currentThread,
			Instruction executedInstruction) {
		// System.out.println("LTNR: choiceGeneratorRegistered");		
	}

	@Override
	public void choiceGeneratorSet(VM vm, ChoiceGenerator<?> newCG) {
		// System.out.println("LTNR: choiceGeneratorSet");

		if (out == null) {
			out = new BufferedWriter(fstream);
			// System.out.println("out is null");
		}
		try {
			out.newLine();
			out.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> currentCG) {
		// System.out.println("LTNR: choiceGeneratorAdvanced :id="+vm.getChoiceGenerator().getChoiceType());
		System.out.println("getInsn = " + vm.getChoiceGenerator().getInsn());
		System.out.println("PATH_START");
		/** create event : PATH_START **/
		eventKind = EventKind.PATH_START;
		try {

			if (!isPathStarted)
				isPathStarted = true;
			fstream.write("event\n");
			if (treePath.isEmpty()) {
				fstream.write("parentid=-1" + "\n");
			} else {
				fstream.write("parentid=" + treePath.peek() + "\n");
			}
			treePath.push(++id);
			--thisid;
			fstream.write("id=" + id + "\n");
			fstream.write("thisid=" + thisid + "\n");
			fstream.write("thread=" + vm.getLastTransition().getThreadInfo().getId() + "\n");
			fstream.write("kind=Path Start" + "\n");
			fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
			fstream.write("line=" + vm.getCurrentThread().getLine() + "\n");
			fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
			/** for details **/
			fstream.write("totalNumOfChoices="
					+ vm.getChoiceGenerator().getTotalNumberOfChoices() + "\n");
			fstream.write("sourceLocation="
					+ vm.getChoiceGenerator().getSourceLocation() + "\n");
			fstream.write("choiceType="
					+ vm.getChoiceGenerator().getSourceLocation() + "\n");
			fstream.write("instructionCG=" + vm.getChoiceGenerator().getInsn()
					+ "\n");

			Object oVal = vm.getChoiceGenerator().getNextChoice();
			if (oVal instanceof ThreadInfo) {
				ThreadInfo th = (ThreadInfo) oVal;
				blThreadChoice = true;
				fstream.write("nextChoice=" + th.getName() + "|"
						+ th.getStateName() + "\n");
				fstream.write("varToBeChanged=Th\n");
				fstream.write("endevent\n");
				fstream.flush();
			} else {
				fstream.write("nextChoice="
						+ vm.getChoiceGenerator().getNextChoice() + "\n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/** code to get sf on choice generator advance **/
		if (sf != null) {
			if (sf.getLocalVars() != null) {
				LocalVarInfo[] lvi = sf.getLocalVars();
				mpOld.clear();
				for (LocalVarInfo l : lvi) {
					if (l.getName().equalsIgnoreCase("args")
							|| l.getName().startsWith("[L")) {
						continue;
					}

					mpOld.put(l.getName(), sf.getLocalValueObject(l));
					System.out.print(" | " + l.getName() + " :: "
							+ sf.getLocalValueObject(l));
				}
			}
		}
		
	}

	@Override
	public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> processedCG) {
		// System.out.println("LTNR: choiceGeneratorProcessed");		
	}

	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		//String strFullMethodName = vm.getLastMethodInfo().getFullName();
		//newjivejpf
		String strFullMethodName = enteredMethod.getFullName();
		
		if (strFullMethodName.contains(".run()V")) {
			System.out.println("- returning");
			return;

		}

		if (isRelevantMethod(strFullMethodName)) {

			//MethodInfo mi = vm.getLastMethodInfo();
			//ElementInfo ei = vm.getLastElementInfo();
			//newjivejpf
			MethodInfo mi = enteredMethod;
			ElementInfo ei = currentThread.getThisElementInfo();

			
			int iObjRef = ei.getObjectRef();
			ElementInfo objInfo = vm.getElementInfo(iObjRef);
			
			
			//ThreadInfo ti = vm.getLastThreadInfo();
			//newjivejpf
			ThreadInfo ti = currentThread;
			
			String strTarget = "";
			String strSignature = "";

			try {

				if (strFullMethodName.contains(".")) {
					strTarget = strFullMethodName.substring(
							strFullMethodName.lastIndexOf(".") + 1,
							strFullMethodName.indexOf("("));

					strSignature = strTarget
							+ ":"
							+ ti.getId()
							+ ";/"
							+ strFullMethodName.substring(
									strFullMethodName.indexOf(".") + 1,
									strFullMethodName.length());
					strTarget = strTarget.replace("$", "#");
					strTarget = strTarget + ":"
							+ ti.getId();
				}

				fstream.write("event\n");
				if (treePath.isEmpty()) {
					methodCall.push("SYSTEM");
					fstream.write("parentid=-1" + "\n");
				} else {
					fstream.write("parentid=" + treePath.peek() + "\n");
				}
				id++;
				fstream.write("id=" + id + "\n");
				fstream.write("thread=" + ti.getId() + "\n");
				fstream.write("**Normal Method Call\n");
				fstream.write("kind=Method Call\n");
				fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
				fstream.write("line=" + mi.getLineNumber(mi.getLastInsn())
						+ "\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("object=" + iObjRef + "\n");

				String caller = null;
				if (methodCall.isEmpty()) {
					caller = "SYSTEM";
					fstream.write("caller=" + caller + "\n");
				} else {

					String strFullCallerName = methodCall.peek().toString();
					int s = strFullCallerName.lastIndexOf(".");
					String t = strFullCallerName.substring(0, s);
					String className = null;
					if (t.contains(".")) {
						className = t.substring(t.lastIndexOf(".") + 1,
								t.length());
					} else {
						className = t;
					}
					String g = strFullCallerName.substring(s + 1,
							strFullCallerName.indexOf("("));
					caller = className + "#" + g;
					fstream.write("caller=" + caller + "\n");
				}

				// for target
				int s = strFullMethodName.lastIndexOf(".");
				String t = strFullMethodName.substring(0, s);
				String className = null;
				if (t.contains(".")) {
					className = t.substring(t.lastIndexOf(".") + 1, t.length());
				} else {
					className = t;
				}
				String g = strFullMethodName.substring(s + 1,
						strFullMethodName.indexOf("("));
				String target = className + "#" + g;
				strSignature = className;
				if (ti.getId() > 0) {
					strSignature = strSignature
							+ ":"
							+ ti.getId()
							+ ";/"
							+ strFullMethodName.substring(
									strFullMethodName.indexOf(".") + 1,
									strFullMethodName.length());
				} else {
					strSignature = strSignature
							+ ";/"
							+ strFullMethodName.substring(
									strFullMethodName.indexOf(".") + 1,
									strFullMethodName.length());
				}

				fstream.write("target=" + target + "\n");
				fstream.write("signature=" + strSignature + "\n");
				fstream.write("endevent\n");
				// print
				System.out.println("METHOD_ENETERED :: caller = " + caller);
				System.out.println("METHOD_ENETERED :: target = " + target);

				// METHOD_ENTERED
				fstream.write("event\n");
				if (treePath.isEmpty()) {
					methodCall.push("SYSTEM");
					fstream.write("parentid=-1" + "\n");
				} else {
					fstream.write("parentid=" + treePath.peek() + "\n");
				}
				id++;
				fstream.write("id=" + id + "\n");
				fstream.write("thread=" + ti.getId() + "\n");
				// fstream.write("thread=0\n");
				fstream.write("kind=Method Entered\n");
				fstream.write("file=" + vm.getPath().getApplication() + ".java" + "\n");
				fstream.write("line=" + mi.getLineNumber(mi.getLastInsn())
						+ "\n");
				fstream.write("timestamp=" + System.currentTimeMillis() + "\n");
				fstream.write("endevent\n");
				// END METHOD_ENTERED

				methodCall.push(strFullMethodName);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
				
	}

	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {

		//MethodInfo mi = vm.getLastMethodInfo();
		//String strFullMethodName = mi.getFullName();
		//newjivejpf
		MethodInfo mi = exitedMethod;
		String strFullMethodName = mi.getFullName();
		ThreadInfo ti = currentThread;

		if (!methodCall.isEmpty()) {
			if (methodCall.peek() != "SYSTEM") {

				if (!methodCall.peek().toString()
						.contains(".main([Ljava/lang/String;)V")) {

					Object o = methodCall.peek();
					String peekMethod = o.toString();
					if (peekMethod.equalsIgnoreCase(strFullMethodName)) {
						try {
							// METHOD_EXIT
							fstream.write("event\n");
							if (treePath.isEmpty()) {
								methodCall.push("SYSTEM");
								fstream.write("parentid=-1" + "\n");
							} else {
								fstream.write("parentid=" + treePath.peek()
										+ "\n");
							}
							id++;
							fstream.write("id=" + id + "\n");
							fstream.write("thread="
									+ ti.getId() + "\n");
							fstream.write("kind=Method Exit\n");
							fstream.write("file=" + vm.getPath().getApplication()
									+ ".java" + "\n");
							fstream.write("line="
									+ mi.getLineNumber(mi.getLastInsn()) + "\n");
							fstream.write("timestamp="
									+ System.currentTimeMillis() + "\n");
							int s = strFullMethodName.lastIndexOf(".");
							String t = strFullMethodName.substring(0, s);
							String className = null;
							if (t.contains(".")) {
								className = t.substring(t.lastIndexOf(".") + 1,
										t.length());
							} else {
								className = t;
							}
							String g = strFullMethodName.substring(s + 1,
									strFullMethodName.indexOf("("));
							String target = className + "#" + g;
							fstream.write("returner=" + target + "\n");
							System.out.println("strFullMethodName = "
									+ strFullMethodName);
							fstream.write("endevent\n");
							// END METHOD_EXIT

						} catch (Exception ex) {
							ex.printStackTrace();
						}
						methodCall.pop();
						System.out.println();
						System.out.println("METHOD_EXIT = " + mi.getFullName());
					}
				}
			}
		}
	}

}