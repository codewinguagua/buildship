/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Simon Scholz (vogella GmbH) - initial API and implementation and initial documentation
 *     Ian Stewart-Binks (Red Hat Inc.) - Bug 473862 - F5 key shortcut doesn't refresh project folder contents
 *     Donát Csikós (Gradle Inc.) - Bug 471786
 */

package org.eclipse.buildship.ui.workspace;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.buildship.core.CorePlugin;
import org.eclipse.buildship.core.configuration.GradleProjectNature;
import org.eclipse.buildship.core.util.collections.AdapterFunction;
import org.eclipse.buildship.core.workspace.GradleBuild;
import org.eclipse.buildship.core.workspace.NewProjectHandler;

/**
 * Collects all selected, Gradle-aware {@link IProject} instances and schedules a
 * {@link SynchronizeGradleProjectsJob} to refresh these projects.
 *
 * @see SynchronizeGradleProjectsJob
 */
public final class ProjectSynchronizer {

    public static void execute(final ExecutionEvent event) {
        Set<IProject> selectedProjects = collectSelectedProjects(event);
        if (selectedProjects.isEmpty()) {
            return;
        }

        Set<GradleBuild> gradleBuilds = Sets.newLinkedHashSet();
        for (IProject selecteProject : selectedProjects) {
            Optional<GradleBuild> gradleBuild = CorePlugin.gradleWorkspaceManager().getGradleBuild(selecteProject);
            if (gradleBuild.isPresent()) {
                gradleBuilds.add(gradleBuild.get());
            }
        }

        for (GradleBuild gradleBuild : gradleBuilds) {
            gradleBuild.synchronize(NewProjectHandler.IMPORT_AND_MERGE);
        }
    }

    private static Set<IProject> collectSelectedProjects(ExecutionEvent event) {
        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection) currentSelection;
            return collectGradleProjects((List<?>) selection.toList());
        } else {
            IEditorInput editorInput = HandlerUtil.getActiveEditorInput(event);
            if (editorInput instanceof FileEditorInput) {
                IFile file = ((FileEditorInput) editorInput).getFile();
                return collectGradleProjects(ImmutableList.of(file));
            } else {
                return ImmutableSet.of();
            }
        }
    }

    private static Set<IProject> collectGradleProjects(List<?> candidates) {
        Set<IProject> projects = Sets.newLinkedHashSet();
        AdapterFunction<IResource> adapterFunction = AdapterFunction.forType(IResource.class);
        for (Object candidate : candidates) {
            IResource resource = adapterFunction.apply(candidate);
            if (resource != null) {
                IProject project = resource.getProject();
                if (GradleProjectNature.isPresentOn(project)) {
                    projects.add(project);
                }
            }
        }
        return projects;
    }

}
