/*
 * Copyright (c) 2018, Psikoi <https://github.com/Psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.worldhopper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;

@Slf4j
class WorldSwitcherPanel extends PluginPanel
{
	private static final Color ODD_ROW = new Color(44, 44, 44);

	private static final int WORLD_COLUMN_WIDTH = 60;
	private static final int PLAYERS_COLUMN_WIDTH = 40;

	private final JPanel listContainer = new JPanel();

	private WorldTableHeader worldHeader;
	private WorldTableHeader playersHeader;
	private WorldTableHeader activityHeader;

	private WorldOrder orderIndex = WorldOrder.WORLD;
	private boolean ascendingOrder = true;

	private List<World> worlds;
	private WorldHopperPlugin plugin;

	WorldSwitcherPanel(WorldHopperPlugin plugin)
	{
		this.plugin = plugin;

		setBorder(null);
		setLayout(new DynamicGridLayout(0, 1));

		JPanel headerContainer = buildHeader();

		listContainer.setLayout(new GridLayout(0, 1));

		add(headerContainer);
		add(listContainer);
	}

	void updateList()
	{
		worlds.sort((w1, w2) ->
		{
			switch (orderIndex)
			{
				case WORLD:
					return Integer.compare(w1.getId(), w2.getId()) * (ascendingOrder ? 1 : -1);
				case PLAYERS:
					return Integer.compare(w1.getPlayers(), w2.getPlayers()) * (ascendingOrder ? 1 : -1);
				case ACTIVITY:
					return w1.getActivity().compareTo(w2.getActivity()) * (ascendingOrder ? 1 : -1);
				default:
					return 0;

			}
		});

		worlds.sort((w1, w2) ->
		{
			boolean b1 = plugin.isFavorite(w1);
			boolean b2 = plugin.isFavorite(w2);
			return Boolean.compare(b2, b1);
		});

		listContainer.removeAll();

		for (int i = 0; i < worlds.size(); i++)
		{
			World world = worlds.get(i);
			listContainer.add(buildRow(world, i % 2 == 0, world.getId() == plugin.getCurrentWorld(), plugin.isFavorite(world)));
		}

		listContainer.revalidate();
		listContainer.repaint();
	}

	void populate(List<World> worlds)
	{
		this.worlds = new ArrayList<>(worlds);
		updateList();
	}

	private void orderBy(WorldOrder order)
	{
		worldHeader.highlight(false, ascendingOrder);
		playersHeader.highlight(false, ascendingOrder);
		activityHeader.highlight(false, ascendingOrder);

		switch (order)
		{
			case WORLD:
				worldHeader.highlight(true, ascendingOrder);
				break;
			case PLAYERS:
				playersHeader.highlight(true, ascendingOrder);
				break;
			case ACTIVITY:
				activityHeader.highlight(true, ascendingOrder);
				break;
		}

		orderIndex = order;
		updateList();
	}

	/**
	 * Builds the entire table header.
	 */
	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		JPanel leftSide = new JPanel(new BorderLayout());

		worldHeader = new WorldTableHeader("World", orderIndex == WorldOrder.WORLD, ascendingOrder);
		worldHeader.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 0));
		worldHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				ascendingOrder = orderIndex != WorldOrder.WORLD || !ascendingOrder;
				orderBy(WorldOrder.WORLD);
			}
		});

		playersHeader = new WorldTableHeader("#", orderIndex == WorldOrder.PLAYERS, ascendingOrder);
		playersHeader.setPreferredSize(new Dimension(PLAYERS_COLUMN_WIDTH, 0));
		playersHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{

				ascendingOrder = orderIndex != WorldOrder.PLAYERS || !ascendingOrder;
				orderBy(WorldOrder.PLAYERS);
			}
		});

		activityHeader = new WorldTableHeader("Activity", orderIndex == WorldOrder.ACTIVITY, ascendingOrder);
		activityHeader.setBorder(new EmptyBorder(3, 5, 3, 5));
		activityHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				ascendingOrder = orderIndex != WorldOrder.ACTIVITY || !ascendingOrder;
				orderBy(WorldOrder.ACTIVITY);
			}
		});

		leftSide.add(worldHeader, BorderLayout.WEST);
		leftSide.add(playersHeader, BorderLayout.EAST);

		header.add(leftSide, BorderLayout.WEST);
		header.add(activityHeader, BorderLayout.CENTER);

		return header;
	}

	/**
	 * Builds a table row, that displays the world's information.
	 */
	private JPanel buildRow(World world, boolean stripe, boolean current, boolean favorite)
	{
		JPanel row = new WorldTableRow(world, current, favorite,
			world1 ->
			{
				plugin.hopTo(world1);
			},
			(world12, add) ->
			{
				if (add)
				{
					plugin.addToFavorites(world12);
				}
				else
				{
					plugin.removeFromFavorites(world12);
				}

				updateList();
			}
		);
		row.setBackground(stripe ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
		return row;
	}

	/**
	 * Enumerates the multiple ordering options for the world list.
	 */
	private enum WorldOrder
	{
		WORLD,
		PLAYERS,
		ACTIVITY,
		PING
	}
}
