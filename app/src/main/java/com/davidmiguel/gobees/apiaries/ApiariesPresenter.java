package com.davidmiguel.gobees.apiaries;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.davidmiguel.gobees.addeditapiary.AddEditApiaryActivity;
import com.davidmiguel.gobees.data.model.Apiary;
import com.davidmiguel.gobees.data.source.GoBeesDataSource;
import com.davidmiguel.gobees.data.source.cache.GoBeesRepository;
import com.davidmiguel.gobees.data.source.local.DataGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;

/**
 * Listens to user actions from the UI ApiariesFragment, retrieves the data and updates the
 * UI as required.
 */
class ApiariesPresenter implements ApiariesContract.Presenter {

    private static final int MIN_UPDATE_WEATHER = 15;

    private GoBeesRepository goBeesRepository;
    private ApiariesContract.View view;

    /**
     * Force update the first time.
     */
    private boolean firstLoad = true;

    ApiariesPresenter(GoBeesRepository goBeesRepository, ApiariesContract.View view) {
        this.goBeesRepository = goBeesRepository;
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void result(int requestCode, int resultCode) {
        // If a apiary was successfully added, show snackbar
        if (AddEditApiaryActivity.REQUEST_ADD_APIARY == requestCode && Activity.RESULT_OK == resultCode) {
            view.showSuccessfullySavedMessage();
        }
    }

    @Override
    public void loadApiaries(boolean forceUpdate) {
        // Force update the first time
        forceUpdate = forceUpdate || firstLoad;
        firstLoad = false;
        // Show progress indicator
        view.setLoadingIndicator(true);
        // Refresh data if needed
        if (forceUpdate) {
            goBeesRepository.refreshApiaries();
        }
        // Get apiaires
        goBeesRepository.getApiaries(new GoBeesDataSource.GetApiariesCallback() {

            @Override
            public void onApiariesLoaded(List<Apiary> apiaries) {
                // The view may not be able to handle UI updates anymore
                if (!view.isActive()) {
                    return;
                }
                // Hide progress indicator
                view.setLoadingIndicator(false);
                // Process apiaries
                if (apiaries.isEmpty()) {
                    // Show a message indicating there are no apiaries
                    view.showNoApiaries();
                } else {
                    // Show the list of apiaries
                    view.showApiaries(apiaries);
                    // Check whether current weather is up to date
                    checkCurrentWeather(apiaries);
                }
            }

            @Override
            public void onDataNotAvailable() {
                // The view may not be able to handle UI updates anymore
                if (!view.isActive()) {
                    return;
                }
                view.showLoadingApiariesError();
            }
        });
    }

    @Override
    public void addEditApiary(long apiaryId) {
        view.showAddEditApiary(apiaryId);
    }

    @Override
    public void openApiaryDetail(@NonNull Apiary requestedApiary) {
        view.showApiaryDetail(requestedApiary.getId());
    }

    @Override
    public void deleteApiary(@NonNull Apiary apiary) {
        // Show progress indicator
        view.setLoadingIndicator(true);
        // Delete apiary
        goBeesRepository.deleteApiary(apiary.getId(), new GoBeesDataSource.TaskCallback() {
            @Override
            public void onSuccess() {
                // The view may not be able to handle UI updates anymore
                if (!view.isActive()) {
                    return;
                }
                // Refresh recordings
                loadApiaries(true);
                // Show success message
                view.showSuccessfullyDeletedMessage();
            }

            @Override
            public void onFailure() {
                // The view may not be able to handle UI updates anymore
                if (!view.isActive()) {
                    return;
                }
                // Hide progress indicator
                view.setLoadingIndicator(false);
                // Show error
                view.showDeletedErrorMessage();
            }
        });
    }

    // TODO eliminar generar y eliminar datos
    @Override
    public void generateData() {
        DataGenerator dataGenerator = new DataGenerator(goBeesRepository);
        dataGenerator.generateData(1);
        loadApiaries(false);
    }

    @Override
    public void deleteData() {
        goBeesRepository.deleteAll();
        loadApiaries(false);
    }

    @Override
    public void start() {
        loadApiaries(false);
    }

    /**
     * Checks whether current weather is up to date (less than 15min old).
     * If not, orders to update the current weather in all apiaries.
     *
     * @param apiaries list of apiaries.
     */
    private void checkCurrentWeather(List<Apiary> apiaries) {
        List<Apiary> apiariesToUpdate = new ArrayList<>();
        Date now = new Date();
        // Check dates
        for (Apiary apiary : apiaries) {
            if (apiary.hasLocation()) {
                if (apiary.getCurrentWeather() != null) {
                    // Check time
                    Date weatherDate = apiary.getCurrentWeather().getTimestamp();
                    long minFromLastUpdate = (now.getTime() - weatherDate.getTime()) / 60000;
                    if (minFromLastUpdate >= MIN_UPDATE_WEATHER) {
                        apiariesToUpdate.add(apiary);
                    }
                } else {
                    apiariesToUpdate.add(apiary);
                }

            }
        }
        // Update weather if needed
        if (apiariesToUpdate.size() > 0) {
            goBeesRepository.updateApiariesCurrentWeather(apiariesToUpdate, new GoBeesDataSource.TaskCallback() {
                @Override
                public void onSuccess() {
                    view.notifyApiariesUpdated();
                    view.showSuccessfullyWeatherUpdatedMessage();
                }

                @Override
                public void onFailure() {
                    view.showWeatherUpdateErrorMessage();
                }
            });
        }

    }
}