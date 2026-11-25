    suspend fun applyBusinessCatalog(
        businessType: BusinessType,
        clearExistingProducts: Boolean = true
    ): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val presets = BusinessCatalog.presetsFor(businessType)

            localDb.withTransaction {
                if (clearExistingProducts) {
                    val existing = dao.getAllProductsOnce()
                    for (prod in existing) {
                        dao.deleteProductById(prod.id)
                    }
                }

                val entities = presets.mapIndexed { index, item ->
                    val resolvedCategory = runCatching {
                        ProductCategory.valueOf(item.categoryName.uppercase(Locale.US))
                    }.getOrDefault(ProductCategory.OTHER)

                    ProductEntity(
                        id = "preset_${businessType.code}_${index + 1}_${newId().take(6)}",
                        nameUrdu = item.nameUrdu.trim().take(120),
                        nameEnglish = item.nameEnglish.trim().take(120),
                        category = resolvedCategory.name,
                        unit = item.unit.trim().ifBlank { "piece" }.take(30),
                        costPrice = item.costPrice.coerceIn(0.0, 1_000_000.0),
                        sellingPrice = item.sellingPrice.coerceIn(0.0, 1_000_000.0),
                        marginPercent = 0.0,
                        autoPriceEnabled = false,
                        stockQty = item.initialStock.coerceIn(0.0, 1_000_000.0),
                        lowStockThreshold = item.lowStockThreshold.coerceIn(0.0, 100_000.0),
                        barcode = "",
                        createdAtMs = now + index,
                        updatedAtMs = now + index
                    )
                }

                if (entities.isNotEmpty()) {
                    dao.upsertProducts(entities)
                }
            }

            Result.success(presets.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply business catalog for ${businessType.code}", e)
            Result.failure(e)
        }
    }
