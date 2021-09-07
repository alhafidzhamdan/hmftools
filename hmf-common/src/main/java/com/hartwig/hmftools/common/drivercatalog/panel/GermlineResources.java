package com.hartwig.hmftools.common.drivercatalog.panel;

import static htsjdk.tribble.AbstractFeatureReader.getFeatureReader;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;

final class GermlineResources
{
    private GermlineResources()
    {
    }

    @NotNull
    static List<VariantContext> whitelist37() throws IOException
    {
        return resource(resourceURL("/drivercatalog/GermlineHotspots.whitelist.37.vcf"));
    }

    @NotNull
    static List<VariantContext> whitelist38() throws IOException
    {
        return resource(resourceURL("/drivercatalog/GermlineHotspots.whitelist.38.vcf"));
    }

    @NotNull
    static List<VariantContext> blacklist37() throws IOException
    {
        return resource(resourceURL("/drivercatalog/GermlineHotspots.blacklist.37.vcf"));
    }

    @NotNull
    static List<VariantContext> blacklist38() throws IOException
    {
        return resource(resourceURL("/drivercatalog/GermlineHotspots.blacklist.38.vcf"));
    }

    @NotNull
    private static List<VariantContext> resource(@NotNull final String file) throws IOException
    {
        return getFeatureReader(file, new VCFCodec(), false).iterator().toList();
    }

    @NotNull
    private static String resourceURL(@NotNull String location)
    {
        return DriverGenePanelConversion.class.getResource(location).toString();
    }
}
