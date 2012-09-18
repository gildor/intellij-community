package org.jetbrains.jps.cmdline;

import org.jetbrains.jps.incremental.BuildLoggingManager;
import org.jetbrains.jps.incremental.CompilerEncodingConfiguration;
import org.jetbrains.jps.incremental.ModuleRootsIndex;
import org.jetbrains.jps.incremental.artifacts.ArtifactRootsIndex;
import org.jetbrains.jps.incremental.fs.BuildFSState;
import org.jetbrains.jps.incremental.storage.BuildDataManager;
import org.jetbrains.jps.incremental.storage.BuildTargetsState;
import org.jetbrains.jps.incremental.storage.ProjectTimestamps;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
* @author Eugene Zhuravlev
*         Date: 1/8/12
*/
public final class ProjectDescriptor {
  public final JpsProject jpsProject;
  public final JpsModel jpsModel;
  public final BuildFSState fsState;
  public final ProjectTimestamps timestamps;
  public final BuildDataManager dataManager;
  private final BuildLoggingManager myLoggingManager;
  private final BuildTargetsState myTargetsState;
  public ModuleRootsIndex rootsIndex;
  private final ArtifactRootsIndex myArtifactRootsIndex;
  private int myUseCounter = 1;
  private Set<JpsSdk<?>> myProjectJavaSdks;
  private CompilerEncodingConfiguration myEncodingConfiguration;

  public ProjectDescriptor(JpsModel jpsModel,
                           BuildFSState fsState,
                           ProjectTimestamps timestamps,
                           BuildDataManager dataManager,
                           BuildLoggingManager loggingManager,
                           final ModuleRootsIndex moduleRootsIndex,
                           final BuildTargetsState targetsState, final ArtifactRootsIndex artifactRootsIndex) {
    this.jpsModel = jpsModel;
    this.jpsProject = jpsModel.getProject();
    this.fsState = fsState;
    this.timestamps = timestamps;
    this.dataManager = dataManager;
    myLoggingManager = loggingManager;
    rootsIndex = moduleRootsIndex;
    myArtifactRootsIndex = artifactRootsIndex;
    myProjectJavaSdks = new HashSet<JpsSdk<?>>();
    myEncodingConfiguration = new CompilerEncodingConfiguration(jpsModel, rootsIndex);
    for (JpsModule module : jpsProject.getModules()) {
      final JpsSdk<?> sdk = module.getSdk(JpsJavaSdkType.INSTANCE);
      if (sdk != null && !myProjectJavaSdks.contains(sdk) && sdk.getVersionString() != null && sdk.getHomePath() != null) {
        myProjectJavaSdks.add(sdk);
      }
    }
    myTargetsState = targetsState;
  }

  public BuildTargetsState getTargetsState() {
    return myTargetsState;
  }

  public CompilerEncodingConfiguration getEncodingConfiguration() {
    return myEncodingConfiguration;
  }

  public Set<JpsSdk<?>> getProjectJavaSdks() {
    return myProjectJavaSdks;
  }

  public BuildLoggingManager getLoggingManager() {
    return myLoggingManager;
  }

  public ArtifactRootsIndex getArtifactRootsIndex() {
    return myArtifactRootsIndex;
  }

  public synchronized void incUsageCounter() {
    myUseCounter++;
  }

  public void release() {
    boolean shouldClose;
    synchronized (this) {
      --myUseCounter;
      shouldClose = myUseCounter == 0;
    }
    if (shouldClose) {
      try {
        timestamps.close();
      }
      finally {
        try {
          dataManager.close();
        }
        catch (IOException e) {
          e.printStackTrace(System.err);
        }
      }
    }
  }
}
