package io.vanillabp.cockpit.workflowlist;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
public class WorkflowlistService {

    @Autowired
    private Logger logger;

    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.asc("createdAt").nullsLast());

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ReactiveChangeStreamUtils changeStreamUtils;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    private Disposable dbChangesSubscription;

    public Mono<Boolean> createWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return Mono.just(Boolean.FALSE);
        }

        return workflowRepository
                .save(workflow)
                .doOnNext(item -> microserviceProxyRegistry
                        .registerMicroservice(
                                item.getWorkflowModule(),
                                item.getWorkflowModuleUri()))
                .map(item -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
    }

    public Mono<Workflow> getWorkflow(
            final String workflowId) {

        return workflowRepository.findById(workflowId);

    }

    public Mono<Page<Workflow>> getWorkflows(
            final int pageNumber,
            final int pageSize) {

        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(DEFAULT_SORT);

        return workflowRepository
                .findAllBy(pageRequest)
                .collectList()
                .zipWith(workflowRepository.countAll())
                .map(t -> new PageImpl<>(t.getT1(), pageRequest, t.getT2()));

    }


    public Mono<Page<Workflow>> getWorkflowsUpdated(
            final int size,
            final Collection<String> knownWorkflowIds) {

        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(DEFAULT_SORT);

        final var workflows = workflowRepository.findAllIds(pageRequest);

        return workflows
                .flatMap(workflow -> {
                    if (knownWorkflowIds.contains(workflow.getId())) {
                        return Mono.just(workflow);
                    }
                    return workflowRepository.findById(workflow.getId());
                })
                .collectList()
                .zipWith(workflowRepository.count())
                .map(t -> new PageImpl<>(
                        t.getT1(),
                        Pageable
                                .ofSize(t.getT1().isEmpty() ? 1 : t.getT1().size())
                                .withPage(0),
                        t.getT2()));

    }


    public Mono<Boolean> updateWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return Mono.just(Boolean.FALSE);
        }

        return workflowRepository
                .save(workflow)
                .onErrorMap(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return null;
                })
                .map(savedWorkflow -> savedWorkflow != null);

    }

    public Mono<Boolean> cancelWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp,
            final String reason) {

        if (workflow == null) {
            Mono.just(Boolean.FALSE);
        }

        workflow.setEndedAt(timestamp);
        workflow.setComment(reason);

        return workflowRepository
                .save(workflow)
                .map(task -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });

    }


    public Mono<Boolean> completeWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp) {

        if (workflow == null) {
            Mono.just(Boolean.FALSE);
        }

        workflow.setEndedAt(timestamp);

        return workflowRepository
                .save(workflow)
                .map(item -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
    }

    @PostConstruct
    public void subscribeToDbChanges() {

        dbChangesSubscription = changeStreamUtils
                .subscribe(Workflow.class)
                .flatMap(workflow -> Mono
                        .fromCallable(() -> WorkflowChangedNotification.build(workflow))
                        .doOnError(e -> logger
                                .warn("Error on processing workflow change-stream "
                                        + "event! Will resume stream.", e))
                        .onErrorResume(Exception.class, e -> Mono.empty()))
                .doOnNext(applicationEventPublisher::publishEvent)
                .subscribe();
/*
        // register all URLs already known
        userTasks
                .findAllWorkflowModulesAndUris()
                .collectList()
                .map(modulesAndUris -> modulesAndUris
                        .stream()
                        .collect(Collectors.toMap(
                                UserTask::getWorkflowModule,
                                UserTask::getWorkflowModuleUri)))
                .doOnNext(microserviceProxyRegistry::registerMicroservice)
                .subscribe();
*/
    }

    @PreDestroy
    public void cleanup() {

        dbChangesSubscription.dispose();

    }

}
