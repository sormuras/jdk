/*
 * Copyright (c) 2001, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_GC_G1_G1HEAPREGIONMANAGER_INLINE_HPP
#define SHARE_GC_G1_G1HEAPREGIONMANAGER_INLINE_HPP

#include "gc/g1/g1HeapRegionManager.hpp"

#include "gc/g1/g1CommittedRegionMap.inline.hpp"
#include "gc/g1/g1HeapRegion.hpp"
#include "gc/g1/g1HeapRegionSet.inline.hpp"

inline bool G1HeapRegionManager::is_available(uint region) const {
  return _committed_map.active(region);
}

inline G1HeapRegion* G1HeapRegionManager::addr_to_region(HeapWord* addr) const {
  assert(addr < heap_end(),
        "addr: " PTR_FORMAT " end: " PTR_FORMAT, p2i(addr), p2i(heap_end()));
  assert(addr >= heap_bottom(),
        "addr: " PTR_FORMAT " bottom: " PTR_FORMAT, p2i(addr), p2i(heap_bottom()));
  return _regions.get_by_address(addr);
}

inline G1HeapRegion* G1HeapRegionManager::at(uint index) const {
  assert(is_available(index), "pre-condition");
  G1HeapRegion* hr = _regions.get_by_index(index);
  assert(hr != nullptr, "sanity");
  assert(hr->hrm_index() == index, "sanity");
  return hr;
}

inline G1HeapRegion* G1HeapRegionManager::at_or_null(uint index) const {
  if (!is_available(index)) {
    return nullptr;
  }
  G1HeapRegion* hr = _regions.get_by_index(index);
  assert(hr != nullptr, "All available regions must have a G1HeapRegion but index %u has not.", index);
  assert(hr->hrm_index() == index, "sanity");
  return hr;
}

inline G1HeapRegion* G1HeapRegionManager::next_region_in_humongous(G1HeapRegion* hr) const {
  uint index = hr->hrm_index();
  assert(is_available(index), "pre-condition");
  assert(hr->is_humongous(), "next_region_in_humongous should only be called for a humongous region.");
  index++;
  if (index < max_num_regions() && is_available(index) && at(index)->is_continues_humongous()) {
    return at(index);
  } else {
    return nullptr;
  }
}

inline void G1HeapRegionManager::insert_into_free_list(G1HeapRegion* hr) {
  _free_list.add_ordered(hr);
}

inline G1HeapRegion* G1HeapRegionManager::allocate_free_regions_starting_at(uint first, uint num_regions) {
  G1HeapRegion* start = at(first);
  _free_list.remove_starting_at(start, num_regions);
  return start;
}

#endif // SHARE_GC_G1_G1HEAPREGIONMANAGER_INLINE_HPP
