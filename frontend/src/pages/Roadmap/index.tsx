import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

import { getProjectStories } from '../../apis/story';
import Header from '../../components/Header';
import StoryList from '../../components/List/Story';
import TaskList from '../../components/List/Task';
import RoadmapStory from '../../components/RoadmapStory';
import SideBar from '../../components/SideBar';
import { StateType } from '../../types/project';
import { StoryType } from '../../types/roadmap';
import { Container, Wrapper, RoadmapArea, ListArea } from './style';

const Roadmap = () => {
    const { state } = useLocation<StateType>();
    const [storyList, setStoryList] = useState<Array<StoryType>>([]);
    const [story, setStory] = useState<number>(-1);
    const [showStory, setShowStory] = useState<boolean>(true);

    const handleStoryVisible = () => setShowStory((prev) => !prev);

    useEffect(() => {
        (async () => {
            const res = await getProjectStories(state.projectId);
            setStoryList(res.data);
        })();
    }, []);

    return (
        <>
            <Header />
            <SideBar />
            <Container>
                <Wrapper>
                    <RoadmapArea>
                        {storyList.map((storyInfo, index) =>
                            storyInfo.startAt && storyInfo.endAt ? (
                                <RoadmapStory
                                    key={index}
                                    storyInfo={storyInfo}
                                    handleStoryVisible={handleStoryVisible}
                                    setStory={setStory}
                                />
                            ) : null,
                        )}
                    </RoadmapArea>
                    <ListArea>
                        {showStory ? (
                            <StoryList
                                handleStoryVisible={handleStoryVisible}
                                setStory={setStory}
                                storyList={storyList}
                            />
                        ) : (
                            <TaskList handleStoryVisible={handleStoryVisible} story={story} />
                        )}
                    </ListArea>
                </Wrapper>
            </Container>
        </>
    );
};

export default Roadmap;
