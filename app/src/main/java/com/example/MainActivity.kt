ShopDestination.CUSTOMER_ORDER -> {
    val effectiveOrderSettings = remember(settings, selectedNearbyShop) {
        val chosen = selectedNearbyShop
        if (chosen != null) {
            settings.copy(
                storeNameUrdu = chosen.nameUrdu,
                storeNameEnglish = chosen.nameEnglish,
                shopPhone = chosen.shopPhone
            )
        } else {
            settings
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NearMeShopsSection(
            language = language,
            settings = settings,
            selectedShop = selectedNearbyShop,
            onSelectShop = { selectedNearbyShop = it },
            onRefreshLocation = { onRefreshComplete ->
                scope.launch {
                    LocationHelper.detectCurrentLocation(context)
                        .onSuccess { loc ->
                            onRefreshComplete(
                                loc.latitude,
                                loc.longitude,
                                loc.addressLine
                            )
                        }
                        .onFailure {
                            viewModel.postMessage(
                                language.pick(
                                    "لوکیشن حاصل نہ ہو سکی — GPS چیک کریں۔",
                                    "Could not detect GPS location."
                                )
                            )
                        }
                }
            },
            onShowMessage = { viewModel.postMessage(it) }
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CustomerOrderScreen(
                language = language,
                settings = effectiveOrderSettings,
                products = products,
                onShowMessage = { viewModel.postMessage(it) }
            )
        }
    }
}
